package org.lastbamboo.common.turn.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.ExecutorThreadModel;
import org.apache.mina.common.IoFilter;
import org.apache.mina.common.IoFilterAdapter;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.SimpleByteBufferAllocator;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;
import org.lastbamboo.common.stun.stack.StunMessageDecoder;
import org.lastbamboo.common.stun.stack.message.BindingRequest;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorAdapter;
import org.lastbamboo.common.stun.stack.message.attributes.turn.ConnectionStatus;
import org.lastbamboo.common.stun.stack.message.turn.AllocateErrorResponse;
import org.lastbamboo.common.stun.stack.message.turn.AllocateRequest;
import org.lastbamboo.common.stun.stack.message.turn.AllocateSuccessResponse;
import org.lastbamboo.common.stun.stack.message.turn.ConnectRequest;
import org.lastbamboo.common.stun.stack.message.turn.ConnectionStatusIndication;
import org.lastbamboo.common.stun.stack.message.turn.DataIndication;
import org.lastbamboo.common.util.CandidateProvider;
import org.lastbamboo.common.util.NotYetImplementedException;
import org.lastbamboo.common.util.RuntimeIoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that handles all responsibilities of a TURN client.  It does this in
 * a couple of ways.  First, it opens a connection to the TURN server and
 * allocates a binding on the TURN server.  Second, it decodes 
 * Data Indication messages arriving from the TURN server. When it receives
 * a message, it creates sockets to the local HTTP server and forwards the
 * data (HTTP data) enclosed in the Data Indication to the local HTTP
 * server.<p>
 * 
 * If this ever loses the connection to the TURN server, it notifies the
 * listener that maintains TURN connections.
 */
public class TcpTurnClient extends StunMessageVisitorAdapter<StunMessage>
    implements TurnClient, IoServiceListener
    {
    
    private final Logger m_log = LoggerFactory.getLogger(getClass());
    
    private InetSocketAddress m_stunServerAddress;
    
    private IoSession m_ioSession;
    private InetSocketAddress m_relayAddress;
    private InetSocketAddress m_mappedAddress;
    private boolean m_receivedAllocateResponse;
    private final TurnClientListener m_turnClientListener;
    private final ProtocolCodecFactory m_dataCodecFactory;
    private int m_totalReadDataBytes;
    private int m_totalReadRawDataBytes;
    private final AtomicBoolean m_connected = new AtomicBoolean(false);
    private final SocketConnector m_connector = new SocketConnector();
    private final CandidateProvider<InetSocketAddress> m_candidateProvider;

    /**
     * Creates a new TURN client with the default provider for server addresses.
     * 
     * @param clientListener The listener for TURN client events.
     * @param codecFactory The codec factory.
     */
    public TcpTurnClient(final TurnClientListener clientListener,
        final ProtocolCodecFactory codecFactory)
        {
        this(clientListener, new TurnServerCandidateProvider(), codecFactory);
        }
    
    /**
     * Creates a new TCP TURN client.
     * 
     * @param clientListener The listener for TURN client events.
     * @param candidateProvider The class that provides TURN candidate 
     * servers.
     * @param codecFactory The codec factory.
     */
    public TcpTurnClient(final TurnClientListener clientListener, 
        final CandidateProvider<InetSocketAddress> candidateProvider,
        final ProtocolCodecFactory codecFactory)
        {
        m_turnClientListener = clientListener;
        m_candidateProvider = candidateProvider;
        m_dataCodecFactory = codecFactory;
        // Configure the MINA buffers for optimal performance.
        ByteBuffer.setUseDirectBuffers(false);
        ByteBuffer.setAllocator(new SimpleByteBufferAllocator());
        }
    
    public void connect()
        {
        if (this.m_connected.get())
            {
            throw new IllegalArgumentException("Already connected...");
            }
        final Collection<InetSocketAddress> candidates = 
            this.m_candidateProvider.getCandidates();

        final InetSocketAddress serverAddress = candidates.iterator().next();
        connect(serverAddress, null);
        synchronized (this.m_connected)
            {
            try
                {
                this.m_connected.wait(20 * 1000);
                }
            catch (final InterruptedException e)
                {
                m_log.error("Interrupted while waiting", e);
                }
            }
        
        if (!isConnected())
            {
            m_log.error("Could not connect or did not get allocate response");
            throw new RuntimeIoException("Could not connect!!");
            }
        }
    
    private void connect(
        final InetSocketAddress stunServerAddress,
        final InetSocketAddress localAddress)
        {
        final StunMessageDecoder decoder = new StunMessageDecoder();
        final IoFilter turnFilter = new IoFilterAdapter()
            {
            @Override
            public void filterWrite(final NextFilter nextFilter, 
                final IoSession session, final WriteRequest writeRequest) 
                throws Exception 
                {
                //m_log.debug("Filtering write: "+writeRequest.getMessage());
                nextFilter.filterWrite(session, writeRequest);
                }
            
            @Override
            public void messageReceived(
                final NextFilter nextFilter, final IoSession session, 
                final Object message) throws Exception
                {
                final ByteBuffer in = (ByteBuffer) message;
                final ProtocolDecoderOutput out = new ProtocolDecoderOutput()
                    {
                    public void flush()
                        {
                        }

                    public void write(final Object msg)
                        {
                        final StunMessage stunMessage = (StunMessage) msg;
                        stunMessage.accept(TcpTurnClient.this);
                        }
                    };
                
                decoder.decode(session, in, out);
                }
            };

        // If TURN is used with ICE, this will be a demultiplexing filter 
        // between STUN and the media stream data. 
        final ProtocolCodecFilter dataFilter =
            new ProtocolCodecFilter(m_dataCodecFactory);

        m_connector.getFilterChain().addLast("stunFilter", turnFilter);
        
        // This is really only used for the encoding.
        m_connector.getFilterChain().addLast("dataFilter", dataFilter);
        
        m_connector.addListener(this);
        
        //m_connectionListener = listener;
        m_stunServerAddress = stunServerAddress;
        final SocketConnectorConfig config = new SocketConnectorConfig();
        config.getSessionConfig().setReuseAddress(true);
        
        final ThreadModel threadModel = 
            ExecutorThreadModel.getInstance("TCP-TURN-Client");
        config.setThreadModel(threadModel);
        //config.setThreadModel(ThreadModel.MANUAL);
        
        final IoHandler ioHandler = new TurnClientIoHandler(this);
        final ConnectFuture connectFuture; 
        if (localAddress == null)
            {
            connectFuture = 
                m_connector.connect(m_stunServerAddress, ioHandler, config);
            }
        else
            {
            connectFuture = 
                m_connector.connect(m_stunServerAddress, localAddress, ioHandler, 
                    config);
            }
        
        final IoFutureListener futureListener = new IoFutureListener()
            {
            public void operationComplete(final IoFuture ioFuture)
                {
                if (!ioFuture.isReady())
                    {
                    m_log.warn("Future not ready?");
                    return;
                    }
                try
                    {
                    m_ioSession = ioFuture.getSession();
                    }
                catch (final RuntimeIOException e)
                    {
                    // This seems to get thrown when we can't connect at all.
                    m_log.warn("Could not connect to TURN server at: {}", 
                        stunServerAddress, e);
                    //m_connectionListener.connectionFailed();
                    return;
                    }
                if (m_ioSession.isConnected())
                    {
                    final AllocateRequest msg = new AllocateRequest();
    
                    m_log.debug ("Sending allocate request to write handler...");
                    m_ioSession.write(msg);
                    }
                else
                    {
                    m_log.debug("Connect failed for: {}", m_ioSession);
                    //m_connectionListener.connectionFailed();
                    }
                }
            };
            
        connectFuture.addListener(futureListener);
        connectFuture.join();
        }
    
    public void close()
        {
        if (this.m_ioSession != null)
            {
            final CloseFuture closeFuture = this.m_ioSession.close();
            closeFuture.join();
            }
        }
    
    public void sendConnectRequest(final InetSocketAddress remoteAddress)
        {
        final ConnectRequest request = new ConnectRequest(remoteAddress);
        this.m_ioSession.write(request);
        }
    
    public InetSocketAddress getRelayAddress()
        {
        return this.m_relayAddress;
        }
    
    public InetSocketAddress getMappedAddress()
        {
        return this.m_mappedAddress;
        }

    @Override
    public StunMessage visitAllocateSuccessResponse(
        final AllocateSuccessResponse response)
        {
        // NOTE: This will get called many times for a single TURN session 
        // between a client and a server because allocate requests are used
        // for keep-alives as well as the initial allocation.
        
        m_log.debug("Got successful allocate response: {}", response);
        // We need to set the relay address before notifying the 
        // listener we're "connected".
        this.m_relayAddress = response.getRelayAddress();
        this.m_mappedAddress = response.getMappedAddress();
        this.m_receivedAllocateResponse = true;
        //this.m_connectionListener.connected(this.m_stunServerAddress);
        this.m_connected.set(true);
        synchronized (this.m_connected)
            {
            this.m_connected.notifyAll();
            }
        return null;
        }
    
    @Override
    public StunMessage visitAllocateErrorResponse(
        final AllocateErrorResponse response)
        {
        m_log.warn("Received an Allocate Response error from the server: "+
            response.getAttributes());
        //this.m_connectionListener.connectionFailed();
        this.m_ioSession.close();
        return null;
        }
    
    @Override
    public StunMessage visitConnectionStatusIndication(
        final ConnectionStatusIndication indication)
        {
        m_log.debug("Visiting connection status message: {}", indication);
        final ConnectionStatus status = indication.getConnectionStatus();
        final InetSocketAddress remoteAddress = indication.getRemoteAddress();
        switch (status)
            {
            case CLOSED:
                m_log.debug("Got connection closed from: "+remoteAddress);
                this.m_turnClientListener.onRemoteAddressClosed(remoteAddress);
                break;
            case ESTABLISHED:
                m_log.debug("Connection established from: "+remoteAddress);
                
                // Create a local connection for the newly established session.
                this.m_turnClientListener.onRemoteAddressOpened(remoteAddress, 
                    this.m_ioSession);
                break;
            case LISTEN:
                m_log.debug("Got server listening for incoming data from: "+
                    remoteAddress);
                break;
            }
        return null;
        }

    @Override
    public StunMessage visitDataIndication(final DataIndication data)
        {
        m_log.debug("Visiting Data Indication message: {}", data);
        m_totalReadDataBytes += data.getTotalLength();
        m_totalReadRawDataBytes += data.getData().length;
        final InetSocketAddress remoteAddress = data.getRemoteAddress();
        try
            {
            m_turnClientListener.onData(remoteAddress, this.m_ioSession, 
                data.getData());
            }
        catch (final Exception e)
            {
            m_log.error("Could not process data: {}", data, e);
            }
        return null;
        }
    
    public void serviceActivated(final IoService service, 
        final SocketAddress serviceAddress, final IoHandler handler, 
        final IoServiceConfig config)
        {
        m_log.debug("Service activated...");
        }

    public void serviceDeactivated(final IoService service, 
        final SocketAddress serviceAddress, final IoHandler handler, 
        final IoServiceConfig config)
        {
        m_log.debug("Service deactivated...");
        }

    public void sessionCreated(final IoSession session)
        {
        m_log.debug("Session created...");
        }

    public void sessionDestroyed(final IoSession session)
        {
        m_log.debug("Session destroyed...");
        if (this.m_receivedAllocateResponse)
            {
            // We're disconnected, so set the allocate response flag to false
            // because the client's current connection, or lack thereof, has
            // not received a response.
            this.m_receivedAllocateResponse = false;
            this.m_connected.set(false);
            //this.m_connectionListener.disconnected();
            }
        
        this.m_turnClientListener.close();
        }
    
    public InetAddress getStunServerAddress()
        {
        return this.m_stunServerAddress.getAddress();
        }

    public InetSocketAddress getHostAddress()
        {
        return (InetSocketAddress) this.m_ioSession.getLocalAddress();
        }

    public InetSocketAddress getServerReflexiveAddress()
        {
        return getMappedAddress();
        }

    public StunMessage write(final BindingRequest request, 
        final InetSocketAddress remoteAddress)
        {
        // TODO Same as above.  We should just send the request to the server,
        // and we should combine the functionality of this class with the 
        // functionality of TcpStunClient.
        m_log.error("Unsupported!!!!!!!");
        throw new NotYetImplementedException("Not implemented.");
        }

    public StunMessage write(final BindingRequest request, 
        final InetSocketAddress remoteAddress, final long rto)
        {
        // TODO Auto-generated method stub
        m_log.error("Unsupported!!!!!!!");
        throw new NotYetImplementedException("Not implemented.");
        }

    public boolean isConnected()
        {
        return this.m_connected.get();
        }
    
    public boolean hostPortMapped()
        {
        // We don't map ports for clients (only for classes that also accept
        // incoming connections).
        return false;
        }

    public void addIoServiceListener(final IoServiceListener serviceListener)
        {
        if (serviceListener == null)
            {
            throw new NullPointerException("Null listener");
            }
        this.m_connector.addListener(serviceListener);
        }
    }

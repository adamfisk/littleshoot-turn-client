package org.lastbamboo.common.turn.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.SystemUtils;
import org.littleshoot.mina.common.ByteBuffer;
import org.littleshoot.mina.common.CloseFuture;
import org.littleshoot.mina.common.ConnectFuture;
import org.littleshoot.mina.common.ExecutorThreadModel;
import org.littleshoot.mina.common.IoFilter;
import org.littleshoot.mina.common.IoFilterAdapter;
import org.littleshoot.mina.common.IoFuture;
import org.littleshoot.mina.common.IoFutureListener;
import org.littleshoot.mina.common.IoHandler;
import org.littleshoot.mina.common.IoService;
import org.littleshoot.mina.common.IoServiceConfig;
import org.littleshoot.mina.common.IoServiceListener;
import org.littleshoot.mina.common.IoSession;
import org.littleshoot.mina.common.RuntimeIOException;
import org.littleshoot.mina.common.SimpleByteBufferAllocator;
import org.littleshoot.mina.common.ThreadModel;
import org.littleshoot.mina.filter.codec.ProtocolCodecFactory;
import org.littleshoot.mina.filter.codec.ProtocolCodecFilter;
import org.littleshoot.mina.filter.codec.ProtocolDecoderOutput;
import org.littleshoot.mina.transport.socket.nio.SocketConnector;
import org.littleshoot.mina.transport.socket.nio.SocketConnectorConfig;
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
import org.littleshoot.util.CandidateProvider;
import org.littleshoot.util.RuntimeIoException;
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
    /*
    public TcpTurnClient(final TurnClientListener clientListener,
        final ProtocolCodecFactory codecFactory)
        {
        this(clientListener, new TurnServerCandidateProvider(), codecFactory);
        }
        */
    
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
    
    public void connect() throws IOException
        {
        if (this.m_connected.get())
            {
            throw new IllegalArgumentException("Already connected...");
            }
        final Collection<InetSocketAddress> candidates = 
            this.m_candidateProvider.getCandidates();
        
        m_log.info("Attempting connections to: {}", candidates);

        for (final InetSocketAddress serverAddress : candidates)
            {
            connect(serverAddress, null);
            synchronized (this.m_connected)
                {
                try
                    {
                    this.m_connected.wait(30 * 1000);
                    }
                catch (final InterruptedException e)
                    {
                    m_log.error("Interrupted while waiting", e);
                    }
                }
            
            if (isConnected())
                {
                m_log.debug("Connected to: {}", serverAddress);
                break;
                }
            }
        if (!isConnected())
            {
            m_log.error("Could not connect or did not get allocate response");
            close();
            throw new IOException("Could not connect to any of: " + candidates);
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
        
        // Java has weird issues with the new networking stack in Windows Vista.
        if (SystemUtils.IS_OS_WINDOWS_VISTA)
            {
            config.getSessionConfig().setKeepAlive(false);
            }
        else
            {
            config.getSessionConfig().setKeepAlive(true);
            }
        config.getSessionConfig().setReuseAddress(true);
        
        final ThreadModel threadModel = 
            ExecutorThreadModel.getInstance("TCP-TURN-Client-"+hashCode());
        config.setThreadModel(threadModel);
        //config.setThreadModel(ThreadModel.MANUAL);
        
        m_log.info("Connection to STUN server here: {}", m_stunServerAddress);
        
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
                    m_log.warn("Could not connect to TURN server at: " + 
                        stunServerAddress, e);
                    //m_connectionListener.connectionFailed();
                    return;
                    }
                if (m_ioSession == null || !m_ioSession.isConnected())
                    {
                    m_log.error("Could not create session");
                    throw new RuntimeIoException("Could not get session");
                    }
                
                // TODO: We should not need this.
                final TurnStunMessageMapper mapper = 
                    new TurnStunMessageMapperImpl();
                m_ioSession.setAttribute("REMOTE_ADDRESS_MAP", mapper);
                final AllocateRequest msg = new AllocateRequest();

                m_log.debug ("Sending allocate request to write handler...");
                m_ioSession.write(msg);
                }
            };
            
        connectFuture.addListener(futureListener);
        connectFuture.join();
        }
    
    public void close()
        {
        m_log.debug("Closing TCP TURN client.");
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
        // TODO We should just send the request to the server,
        // and we should combine the functionality of this class with the 
        // functionality of TcpStunClient.
        // Or is this just handled by IceStunCheckers??
        m_log.error("Unsupported!!!!!!!");
        throw new IllegalStateException("Not implemented.");
        }

    public StunMessage write(final BindingRequest request, 
        final InetSocketAddress remoteAddress, final long rto)
        {
        // See comment above.
        m_log.error("Unsupported!!!!!!!");
        throw new IllegalStateException("Not implemented.");
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

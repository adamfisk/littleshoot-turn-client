package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoConnectorConfig;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;
import org.lastbamboo.client.util.settings.HttpSettings;
import org.lastbamboo.common.stun.stack.decoder.StunMessageDecodingState;
import org.lastbamboo.common.stun.stack.encoder.StunProtocolEncoder;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorAdapter;
import org.lastbamboo.common.stun.stack.message.turn.AllocateRequest;
import org.lastbamboo.common.stun.stack.message.turn.ConnectRequest;
import org.lastbamboo.common.stun.stack.message.turn.ConnectionStatusIndication;
import org.lastbamboo.common.stun.stack.message.turn.DataIndication;
import org.lastbamboo.common.stun.stack.message.turn.SendIndication;
import org.lastbamboo.common.stun.stack.message.turn.SuccessfulAllocateResponse;
import org.lastbamboo.common.util.ConnectionMaintainerListener;
import org.lastbamboo.common.util.mina.MinaUtils;
import org.lastbamboo.common.util.mina.StateMachineProtocolDecoder;
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
public class TurnClientImpl extends StunMessageVisitorAdapter 
    implements TurnClient, IoServiceListener
    {
    
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private ConnectionMaintainerListener<InetSocketAddress> m_listener;
    private final SocketConnector m_connector;
    
    private InetSocketAddress m_turnServerAddress;
    
    private final Map<InetSocketAddress, IoSession> m_addressesToSessions =
        new ConcurrentHashMap<InetSocketAddress, IoSession>();
    private IoSession m_ioSession;
    private InetSocketAddress m_allocatedAddress;
    
    /**
     * This is the limit on the length of the data to encapsulate in a Send
     * Request.  TURN messages cannot be larger than 0xffff, so this leaves 
     * room for other attributes in the message as well as for headers.
     */
    private static final int LENGTH_LIMIT = 0xffff - 1000;

    /**
     * Creates a new TURN client.
     */
    public TurnClientImpl()
        {
        m_connector = new SocketConnector();
        
        // This will encode Allocate Requests and Send Indications.
        final ProtocolEncoder encoder = new StunProtocolEncoder();
        final ProtocolDecoder decoder = 
            new StateMachineProtocolDecoder(new StunMessageDecodingState());
        final ProtocolCodecFilter stunFilter = 
            new ProtocolCodecFilter(encoder, decoder);
        m_connector.getFilterChain().addLast("codec", stunFilter);
        m_connector.addListener(this);
        }
    
    public InetSocketAddress getAllocatedAddress()
        {
        return this.m_allocatedAddress;
        }

    public void connect(
        final ConnectionMaintainerListener<InetSocketAddress> listener, 
        final InetSocketAddress serverAddress)
        {
        m_listener = listener;
        m_turnServerAddress = serverAddress;
        final IoConnectorConfig config = new SocketConnectorConfig();
        final IoHandler handler = new TurnClientIoHandler(this);
        final ConnectFuture connectFuture = 
            m_connector.connect(m_turnServerAddress, handler, config);
        
        final IoFutureListener futureListener = new IoFutureListener()
            {
            public void operationComplete(final IoFuture ioFuture)
                {
                if (!ioFuture.isReady())
                    {
                    LOG.warn("Future not ready?");
                    return;
                    }
                try
                    {
                    m_ioSession = ioFuture.getSession();
                    }
                catch (final RuntimeIOException e)
                    {
                    // This seems to get thrown when we can't connect at all.
                    LOG.warn("Could not connect to TURN server at: {}", 
                        serverAddress, e);
                    return;
                    }
                if (m_ioSession.isConnected())
                    {
                    final AllocateRequest msg = new AllocateRequest();
    
                    LOG.debug ("Sending allocate request to write handler...");
                    m_ioSession.write(msg);
                    }
                else
                    {
                    LOG.debug("Connect failed for: {}", m_ioSession);
                    m_listener.connectionFailed ();
                    }
                }
            };
            
        connectFuture.addListener(futureListener);
        connectFuture.join();
        }
    
    public void close()
        {
        final CloseFuture closeFuture = this.m_ioSession.close();
        closeFuture.join();
        }
    
    public void sendConnectRequest(final InetSocketAddress remoteAddress)
        {
        final ConnectRequest request = new ConnectRequest(remoteAddress);
        this.m_ioSession.write(request);
        }

    public void visitSuccessfulAllocateResponse(
        final SuccessfulAllocateResponse response)
        {
        // We need to set the allocated address before notifying the 
        // listener we're "connected".
        this.m_allocatedAddress = response.getMappedAddress();
        this.m_listener.connected(this.m_turnServerAddress);
        }
    
    public void visitConnectionStatusIndication(
        final ConnectionStatusIndication indication)
        {
        LOG.debug("Visiting connection status message: {}", indication);
        }

    public void visitDataIndication(final DataIndication data)
        {
        LOG.debug("Visiting Data Indication message: {}", data);
        final InetSocketAddress remoteAddress = data.getRemoteAddress();
        final IoSession session = getSessionForRemoteAddress(remoteAddress);
        session.write(ByteBuffer.wrap(data.getData()));
        }

    private IoSession getSessionForRemoteAddress(
        final InetSocketAddress remoteAddress)
        {
        // We don't synchronize here because we're processing data from
        // a single TCP connection.
        if (m_addressesToSessions.containsKey(remoteAddress))
            {
            // This is the connection from the local proxy server to the 
            // local client.  So we're essentially writing to our local
            // we server.
            return m_addressesToSessions.get(remoteAddress);
            }
        else
            {
            LOG.debug("Opening new local socket...");
            final IoConnector connector = new SocketConnector();
            final InetSocketAddress localServer = 
                new InetSocketAddress("127.0.0.1", 
                    HttpSettings.HTTP_PORT.getValue());
            final IoHandler handler = new IoHandlerAdapter() 
                {
                public void messageReceived(final IoSession session, 
                    final Object message) throws Exception
                    {
                    // This is data received from the local HTTP server --
                    // the raw data of an HTTP response.  It might be
                    // larger than the maximum allowed size for TURN messages,
                    // so we make sure to split it up.
                    final ByteBuffer in = (ByteBuffer) message;
                    
                    // Send the data broken up into chunks if necessary.  This 
                    // is because TURN messages cannot be larger than 0xffff.
                    sendSplitBuffers(remoteAddress, in);
                    }
                
                public void sessionClosed(final IoSession session) 
                    throws Exception
                    {
                    m_addressesToSessions.remove(remoteAddress);
                    }
                
                /**
                 * Splits the main read buffer into smaller buffers that will 
                 * fit in TURN messages.
                 * 
                 * @param remoteHost The host the data came from.
                 * @param buffer The main read buffer to split.
                 * @param session The session for reading and writing data.
                 * @param nextFilter The next class for processing the message.
                 */
                private void sendSplitBuffers(
                    final InetSocketAddress remoteHost, final ByteBuffer buffer)
                    {
                    // Break up the data into smaller chunks.
                    final Collection<byte[]> buffers = 
                        MinaUtils.splitToByteArrays(buffer, LENGTH_LIMIT);
                    for (final byte[] data : buffers)
                        {
                        final SendIndication indication = 
                            new SendIndication(remoteHost, data);
                        m_ioSession.write(indication);
                        }
                    }
                };
                
            final ConnectFuture ioFuture = 
                connector.connect(localServer, handler);
            
            // We're just connecting locally, so it should be much quicker 
            // than this unless there's something wrong.
            ioFuture.join(6000);
            final IoSession session = ioFuture.getSession();
            if (!session.isConnected())
                {
                LOG.error("Could not connect to HTTP server!!");
                }
            this.m_addressesToSessions.put(remoteAddress, session);
            return session;
            }
        }

    public void serviceActivated(final IoService service, 
        final SocketAddress serviceAddress, final IoHandler handler, 
        final IoServiceConfig config)
        {
        LOG.debug("Service activated...");
        }

    public void serviceDeactivated(final IoService service, 
        final SocketAddress serviceAddress, final IoHandler handler, 
        final IoServiceConfig config)
        {
        LOG.debug("Service deactivated...");
        }

    public void sessionCreated(final IoSession session)
        {
        LOG.debug("Session created...");
        }

    public void sessionDestroyed(final IoSession session)
        {
        LOG.debug("Session destroyed...");
        this.m_listener.disconnected();
        }

    }

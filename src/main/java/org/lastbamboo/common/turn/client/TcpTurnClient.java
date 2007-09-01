package org.lastbamboo.common.turn.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.ExecutorThreadModel;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoFuture;
import org.apache.mina.common.IoFutureListener;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoService;
import org.apache.mina.common.IoServiceConfig;
import org.apache.mina.common.IoServiceListener;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.RuntimeIOException;
import org.apache.mina.common.ThreadModel;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.socket.nio.SocketConnectorConfig;
import org.lastbamboo.common.stun.client.StunClient;
import org.lastbamboo.common.stun.stack.StunProtocolCodecFactory;
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
import org.lastbamboo.common.util.ConnectionMaintainerListener;
import org.lastbamboo.common.util.ShootConstants;
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
public class TcpTurnClient extends StunMessageVisitorAdapter<Object>
    implements TurnClient, IoServiceListener, TurnLocalSessionListener,
    StunClient
    {
    
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private ConnectionMaintainerListener<InetSocketAddress> m_listener;
    
    private InetSocketAddress m_stunServerAddress;
    
    private final Map<InetSocketAddress, IoSession> m_addressesToSessions =
        new ConcurrentHashMap<InetSocketAddress, IoSession>();
    private IoSession m_ioSession;
    private InetSocketAddress m_relayAddress;
    private InetSocketAddress m_mappedAddress;
    private boolean m_receivedAllocateResponse;

    public void connect(
        final ConnectionMaintainerListener<InetSocketAddress> listener, 
        final InetSocketAddress stunServerAddress)
        {
        final SocketConnector connector = new SocketConnector();
        
        // This will encode Allocate Requests and Send Indications.
        final ProtocolCodecFactory codecFactory = 
            new StunProtocolCodecFactory();
        final ProtocolCodecFilter stunFilter = 
            new ProtocolCodecFilter(codecFactory);
        connector.getFilterChain().addLast("codec", stunFilter);
        connector.addListener(this);
        
        m_listener = listener;
        m_stunServerAddress = stunServerAddress;
        final SocketConnectorConfig config = new SocketConnectorConfig();
        config.getSessionConfig().setReuseAddress(true);
        final ThreadModel threadModel = 
            ExecutorThreadModel.getInstance("TCP-TURN-Client");
        config.setThreadModel(threadModel);
        
        final IoHandler handler = new TurnClientIoHandler(this);
        final ConnectFuture connectFuture = 
            connector.connect(m_stunServerAddress, handler, config);
        
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
                        stunServerAddress, e);
                    m_listener.connectionFailed();
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
                    m_listener.connectionFailed();
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
    
    public InetSocketAddress getRelayAddress()
        {
        return this.m_relayAddress;
        }
    
    public InetSocketAddress getMappedAddress()
        {
        return this.m_mappedAddress;
        }

    @Override
    public Object visitAllocateSuccessResponse(
        final AllocateSuccessResponse response)
        {
        // NOTE: This will get called many times for a single TURN session 
        // between a client and a server because allocate requests are used
        // for keep-alives as well as the initial allocation.
        
        LOG.debug("Got successful allocate response: {}", response);
        // We need to set the relay address before notifying the 
        // listener we're "connected".
        this.m_relayAddress = response.getRelayAddress();
        this.m_mappedAddress = response.getMappedAddress();
        this.m_receivedAllocateResponse = true;
        this.m_listener.connected(this.m_stunServerAddress);
        return null;
        }
    
    @Override
    public Object visitAllocateErrorResponse(
        final AllocateErrorResponse response)
        {
        LOG.warn("Received an Allocate Response error from the server: "+
            response.getAttributes());
        this.m_listener.connectionFailed();
        this.m_ioSession.close();
        return null;
        }
    
    public Object visitConnectionStatusIndication(
        final ConnectionStatusIndication indication)
        {
        LOG.debug("Visiting connection status message: {}", indication);
        final ConnectionStatus status = indication.getConnectionStatus();
        final InetSocketAddress remoteAddress = indication.getRemoteAddress();
        switch (status)
            {
            case CLOSED:
                LOG.debug("Got connection closed from: "+remoteAddress);
                if (!this.m_addressesToSessions.containsKey(remoteAddress))
                    {
                    // This would be odd -- could indicate someone fiddling
                    // with our servers?
                    LOG.warn("We don't know about the remote address: "+
                        remoteAddress);
                    }
                else
                    {
                    LOG.debug("Closing connection to local HTTP server...");
                    final IoSession session = 
                        this.m_addressesToSessions.remove(remoteAddress);
                    
                    // Stop the local session.  In particular, it the session
                    // is in the middle of an HTTP transfer, this will stop
                    // the HTTP server from sending more data to a host that's
                    // no longer there on the other end.
                    session.close();
                    }
                break;
            case ESTABLISHED:
                LOG.debug("Connection established from: "+remoteAddress);
                
                // Create a local connection for the newly established session.
                establishSessionForRemoteAddress(remoteAddress);
                break;
            case LISTEN:
                LOG.debug("Got server listening for incoming data from: "+
                    remoteAddress);
                break;
            }
        return null;
        }

    public Object visitDataIndication(final DataIndication data)
        {
        LOG.debug("Visiting Data Indication message: {}", data);
        final InetSocketAddress remoteAddress = data.getRemoteAddress();
        
        // The session should be there 99% of the time, so we don't need
        // to "establish" it, we just need to get it (since the handling of
        // the ESTABLISHED Connection Status Indication message will have 
        // already created it).  We do, however, include local idle session
        // timeout handling that could have closed the session, so we make
        // sure to establish it if it's not there.  
        //
        // We issue a warning because this could indicate suspect behavior from
        // the remote host.
        if (!m_addressesToSessions.containsKey(remoteAddress))
            {
            LOG.warn("Session for "+remoteAddress+"  timed out earlier??");
            }
        final IoSession session = 
            establishSessionForRemoteAddress(remoteAddress);
        session.write(ByteBuffer.wrap(data.getData()));
        return null;
        }

    private IoSession establishSessionForRemoteAddress(
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
            final ThreadModel threadModel = 
                ExecutorThreadModel.getInstance("TCP-TURN-Client-Local-Socket");
            connector.getDefaultConfig().setThreadModel(threadModel);
            
            final InetSocketAddress localServer = 
                new InetSocketAddress("127.0.0.1", ShootConstants.HTTP_PORT);
            final IoHandler ioHandler = 
                new TurnLocalIoHandler(this, m_ioSession, remoteAddress);
                
            final ConnectFuture ioFuture = 
                connector.connect(localServer, ioHandler);
            
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
        if (this.m_receivedAllocateResponse)
            {
            // We're disconnected, so set the allocate response flag to false
            // because the client's current connection, or lack thereof, has
            // not received a response.
            this.m_receivedAllocateResponse = false;
            this.m_listener.disconnected();
            }
        
        // Now close any of the local "proxied" sockets as well.
        final Collection<IoSession> sessions = 
            this.m_addressesToSessions.values();
        for (final IoSession curSession : sessions)
            {
            curSession.close();
            }
        this.m_addressesToSessions.clear();
        }

    public void onLocalSessionClosed(final InetSocketAddress remoteAddress, 
        final IoSession session)
        {
        // Remove the remote address.  Note the session's already been 
        // closed -- that's why we received this event.  We also have likely
        // already removed it depending on exactly where it arose from, but
        // we remove it again just in case.
        LOG.debug("Received local session closed...");
        this.m_addressesToSessions.remove(remoteAddress);
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
        return null;
        }

    public StunMessage write(final BindingRequest request, 
        final InetSocketAddress remoteAddress, final long rto)
        {
        // TODO Auto-generated method stub
        return null;
        }
    }

package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;
import java.util.Collection;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionUtil;
import org.lastbamboo.common.stun.stack.message.turn.SendIndication;
import org.lastbamboo.common.util.mina.MinaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IoHandler} for local sockets to the HTTP server.  There is one
 * socket/session for each remote host we're exchanging data with.  This
 * effectively mimics the remote host connecting directly to the local
 * HTTP server, with the data already extracted from the TURN messages and
 * forwarded along these sockets.<p>
 * 
 * This class is also responsible for wraping data from the HTTP server
 * in TURN Send Indication messages.
 */
public class TurnLocalIoHandler extends IoHandlerAdapter
    {

    private final Logger LOG = LoggerFactory.getLogger(getClass());
    
    /**
     * This is the limit on the length of the data to encapsulate in a Send
     * Request.  TURN messages cannot be larger than 0xffff, so this leaves 
     * room for other attributes in the message as well as for headers.
     */
    private static final int LENGTH_LIMIT = 0xffff - 1000;
    
    private final InetSocketAddress m_remoteAddress;
    private final IoSession m_ioSession;

    private final TurnLocalSessionListener m_localSessionListener;

    /**
     * Creates a new TURN local IO handler.
     * 
     * @param localSessionListener The class for listening to local session 
     * events.
     * @param ioSession The connection to the TURN server itself.
     * @param remoteAddress The remote host we're exchanging data with.
     */
    public TurnLocalIoHandler(
        final TurnLocalSessionListener localSessionListener, 
        final IoSession ioSession, final InetSocketAddress remoteAddress)
        {
        m_localSessionListener = localSessionListener;
        m_ioSession = ioSession;
        m_remoteAddress = remoteAddress;
        }

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
        sendSplitBuffers(m_remoteAddress, in);
        }
    
    public void sessionClosed(final IoSession session) 
        throws Exception
        {
        // Remember this is only a local "proxied" session.  
        LOG.debug("Received **local** session closed!!");
        m_localSessionListener.onLocalSessionClosed(m_remoteAddress, session);
        }
    
    public void sessionCreated(final IoSession session) 
        throws Exception
        {
        SessionUtil.initialize(session);
        
        // We consider a connection to be idle if there's been no 
        // traffic in either direction for awhile.  
        session.setIdleTime(IdleStatus.BOTH_IDLE, 60 * 10);
        }

    public void sessionIdle(final IoSession session, 
        final IdleStatus status) throws Exception
        {
        // We close idle sessions to make sure we don't consume
        // too many client resources.
        // Note closing the session here will create the 
        // appropriate event handlers to clean up all mappings 
        // and references. 
        session.close();
        }

    public void exceptionCaught(final IoSession session, 
        final Throwable cause) throws Exception
        {
        LOG.error("Error processing data for **local** session: "+
            session, cause);
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
        LOG.debug("Sending split buffers!!");
        // Break up the data into smaller chunks.
        final Collection<byte[]> buffers = 
            MinaUtils.splitToByteArrays(buffer, LENGTH_LIMIT);
        for (final byte[] data : buffers)
            {
            LOG.debug("Sending buffer with capacity: {}", data.length);
            final SendIndication indication = 
                new SendIndication(remoteHost, data);
            m_ioSession.write(indication);
            }
        }
    }

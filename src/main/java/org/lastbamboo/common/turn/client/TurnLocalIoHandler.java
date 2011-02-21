package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;
import java.util.Collection;

import org.lastbamboo.common.stun.stack.message.turn.SendIndication;
import org.littleshoot.util.mina.MinaUtils;
import org.littleshoot.mina.common.ByteBuffer;
import org.littleshoot.mina.common.IdleStatus;
import org.littleshoot.mina.common.IoHandler;
import org.littleshoot.mina.common.IoHandlerAdapter;
import org.littleshoot.mina.common.IoSession;
import org.littleshoot.mina.util.SessionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IoHandler} for local sockets to the HTTP server. There is one
 * socket/session for each remote host we're exchanging data with. This
 * effectively mimics the remote host connecting directly to the local
 * HTTP server, with the data already extracted from the TURN messages and
 * forwarded along these sockets.<p>
 * 
 * This class is also responsible for wrapping data from the HTTP server
 * in TURN Send Indication messages.
 */
public class TurnLocalIoHandler extends IoHandlerAdapter
    {

    private final Logger m_log = LoggerFactory.getLogger(getClass());
    
    /**
     * This is the limit on the length of the data to encapsulate in a Send
     * Request.  TURN messages cannot be larger than 0xffff, so this leaves 
     * room for other attributes in the message as well as for headers.
     */
    private static final int LENGTH_LIMIT = 0xffff - 1000;
    
    private final InetSocketAddress m_remoteAddress;
    private final IoSession m_ioSession;

    /**
     * Creates a new TURN local IO handler.
     * 
     * @param ioSession The connection to the TURN server itself.
     * @param remoteAddress The remote host we're exchanging data with.
     */
    public TurnLocalIoHandler(
        final IoSession ioSession, final InetSocketAddress remoteAddress)
        {
        m_ioSession = ioSession;
        m_remoteAddress = remoteAddress;
        }

    @Override
    public void messageReceived(final IoSession session, final Object message) 
        {
        m_log.debug("Received local data message.");
        // This is data received from the local HTTP server --
        // the raw data of an HTTP response.  It might be
        // larger than the maximum allowed size for TURN messages,
        // so we make sure to split it up.
        final ByteBuffer in = (ByteBuffer) message;
        
        // Send the data broken up into chunks if necessary. This 
        // is because TURN messages cannot be larger than 0xffff.
        sendSplitBuffers(in);
        }
    
    @Override
    public void messageSent(final IoSession session, final Object message) 
        {
        m_log.debug("Sent local TURN message number: {}", 
            session.getWrittenMessages());
        }
    
    @Override
    public void sessionClosed(final IoSession session) 
        {
        // Remember this is only a local "proxied" session.  
        m_log.debug("Received **local** session closed!!");
        }
    
    @Override
    public void sessionCreated(final IoSession session) 
        {
        SessionUtil.initialize(session);
        
        // We consider a connection to be idle if there's been no 
        // traffic in either direction for awhile.  
        //session.setIdleTime(IdleStatus.BOTH_IDLE, 60 * 2);
        }

    @Override
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

    @Override
    public void exceptionCaught(final IoSession session, 
        final Throwable cause) 
        {
        m_log.error("Error processing data for **local** session: "+
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
    private void sendSplitBuffers(final ByteBuffer buffer)
        {
        // Break up the data into smaller chunks.
        final Collection<byte[]> buffers = 
            MinaUtils.splitToByteArrays(buffer, LENGTH_LIMIT);
        m_log.debug("Split single buffer into {}", buffers.size());
        for (final byte[] data : buffers)
            {
            m_log.debug("Sending buffer with capacity: {}", data.length);
            
            //m_log.info("Sending bytes: {}", data);
            final SendIndication indication = 
                new SendIndication(m_remoteAddress, data);
            m_ioSession.write(indication);
            }
        }
    }

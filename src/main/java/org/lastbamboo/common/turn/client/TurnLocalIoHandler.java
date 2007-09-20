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
import org.lastbamboo.common.tcp.frame.TcpFrame;
import org.lastbamboo.common.tcp.frame.TcpFrameEncoder;
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

    public void messageReceived(final IoSession session, final Object message) 
        {
        m_log.debug("Received local data message: {}", message);
        // This is data received from the local HTTP server --
        // the raw data of an HTTP response.  It might be
        // larger than the maximum allowed size for TURN messages,
        // so we make sure to split it up.
        final ByteBuffer in = (ByteBuffer) message;
        
        // Send the data broken up into chunks if necessary.  This 
        // is because TURN messages cannot be larger than 0xffff.
        sendSplitBuffers(in);
        }
    
    public void messageSent(final IoSession session, final Object message) 
        {
        m_log.debug("Sent local TURN message number: {}", 
            session.getWrittenMessages());
        }
    
    public void sessionClosed(final IoSession session) 
        {
        // Remember this is only a local "proxied" session.  
        m_log.debug("Received **local** session closed!!");
        }
    
    public void sessionCreated(final IoSession session) 
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
            final TcpFrame frame = new TcpFrame(data);
            final TcpFrameEncoder encoder = new TcpFrameEncoder();
            final ByteBuffer encodedFrame = encoder.encode(frame);
            
            // TODO: Avoid this extra copy!!
            final byte[] bytes = MinaUtils.toByteArray(encodedFrame);
            m_log.debug("Sending TCP framed data of length: {}", bytes.length);
            final SendIndication indication = 
                new SendIndication(m_remoteAddress, bytes);
            m_ioSession.write(indication);
            }
        }
    }

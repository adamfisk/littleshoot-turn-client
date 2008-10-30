package org.lastbamboo.common.turn.client;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionUtil;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IoHandler} for the TURN client connection to the TURN server.
 */
public class TurnClientIoHandler extends IoHandlerAdapter
    {

    private final Logger LOG = LoggerFactory.getLogger(getClass());
    
    private final StunMessageVisitor<StunMessage> m_visitor;

    /**
     * Creates a new {@link IoHandler} for the TURN proxy/client.
     * 
     * @param visitor The class for visiting read messages.
     */
    public TurnClientIoHandler(final StunMessageVisitor<StunMessage> visitor)
        {
        m_visitor = visitor;
        }

    @Override
    public void messageReceived(final IoSession session, final Object message)
        {
        final StunMessage vsm = (StunMessage) message;
        vsm.accept(this.m_visitor);
        }
    
    @Override
    public void sessionClosed(final IoSession session)
        {
        // This is taken care of through an IoServiceListener.
        }
    
    @Override
    public void sessionCreated(final IoSession session)
        {
        SessionUtil.initialize(session);
        
        // The idle time is in seconds.  Many NATs kill TCP bindings after 
        // about 10 minutes, although there's a lot of variation between
        // implementations.  We use the writer here because I believe many
        // NATs reset their timers only on writes.  At least we can be 
        // relatively sure a write will do the trick, whereas we're not so
        // sure with a read.
        session.setIdleTime(IdleStatus.WRITER_IDLE, 60 * 4);
        }

    @Override
    public void sessionIdle(final IoSession session, final IdleStatus status) 
        {
        LOG.debug("Session idle...closing TURN connection to server...");
        
        //final AllocateRequest request = new AllocateRequest();
        //session.write(request);
        
        // We used to issue a new allocate request in this case to make sure
        // the server keeps the TURN connection up.  This only makes sense
        // for maintaining long-lived TURN connections, though.  In our case,
        // if a connection is idle, it's not being used for a file transfer,
        // so we should close it.
        
        // Note we ideally should not rely on this behavior, instead closing
        // TURN connections whenever we know they're not used.
        session.close();
        }
    
    @Override
    public void exceptionCaught(final IoSession session, final Throwable cause)
        {
        LOG.warn("Caught exception", cause);
        }
    }

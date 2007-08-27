package org.lastbamboo.common.turn.client;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.util.SessionUtil;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitor;
import org.lastbamboo.common.stun.stack.message.VisitableStunMessage;
import org.lastbamboo.common.stun.stack.message.turn.AllocateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IoHandler} for the TURN client connection to the TURN server.
 */
public class TurnClientIoHandler extends IoHandlerAdapter
    {

    private final Logger LOG = LoggerFactory.getLogger(getClass());
    
    private final StunMessageVisitor m_visitor;

    /**
     * Creates a new {@link IoHandler} for the TURN proxy/client.
     * 
     * @param visitor The class for visiting read messages.
     */
    public TurnClientIoHandler(final StunMessageVisitor visitor)
        {
        m_visitor = visitor;
        }

    public void messageReceived(final IoSession session, 
        final Object message) throws Exception
        {
        final VisitableStunMessage vsm = (VisitableStunMessage) message;
        vsm.accept(this.m_visitor);
        }
    
    public void sessionClosed(final IoSession session ) throws Exception
        {
        // This is taken care of through an IoServiceListener.
        }
    
    public void sessionCreated(final IoSession session) throws Exception
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

    public void sessionIdle(final IoSession session, final IdleStatus status) 
        throws Exception
        {
        LOG.debug("Session idle...issue new Allocate Request...");
        final AllocateRequest request = new AllocateRequest();
        session.write(request);
        }
    
    public void exceptionCaught(final IoSession session, final Throwable cause)
        {
        LOG.warn("Caught exception", cause);
        }
    }

package org.lastbamboo.common.turn.client;

import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitor;
import org.lastbamboo.common.stun.stack.message.VisitableStunMessage;

/**
 * {@link IoHandler} for the TURN proxy client.
 */
public class TurnClientIoHandler extends IoHandlerAdapter
    {

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

    public void sessionIdle(final IoSession session, final IdleStatus status) 
        throws Exception
        {
        // TODO: We want to issue a keep alive message here, although we have
        // to tune what is considered "idle".
        }
    }

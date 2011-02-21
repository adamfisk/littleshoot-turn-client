package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;

import org.apache.commons.id.uuid.UUID;
import org.littleshoot.mina.common.ByteBuffer;
import org.littleshoot.mina.common.IoSession;
import org.littleshoot.mina.filter.codec.ProtocolEncoderOutput;
import org.lastbamboo.common.stun.stack.encoder.StunMessageEncoder;
import org.lastbamboo.common.stun.stack.message.BindingErrorResponse;
import org.lastbamboo.common.stun.stack.message.BindingRequest;
import org.lastbamboo.common.stun.stack.message.BindingSuccessResponse;
import org.lastbamboo.common.stun.stack.message.CanceledStunMessage;
import org.lastbamboo.common.stun.stack.message.ConnectErrorStunMessage;
import org.lastbamboo.common.stun.stack.message.NullStunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitor;
import org.lastbamboo.common.stun.stack.message.turn.AllocateErrorResponse;
import org.lastbamboo.common.stun.stack.message.turn.AllocateRequest;
import org.lastbamboo.common.stun.stack.message.turn.AllocateSuccessResponse;
import org.lastbamboo.common.stun.stack.message.turn.ConnectRequest;
import org.lastbamboo.common.stun.stack.message.turn.ConnectionStatusIndication;
import org.lastbamboo.common.stun.stack.message.turn.DataIndication;
import org.lastbamboo.common.stun.stack.message.turn.SendIndication;
import org.littleshoot.util.mina.DemuxableProtocolEncoder;
import org.littleshoot.util.mina.MinaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encodes bytes into STUN messages with the additional step of wrapping 
 * certain messages in TURN Send Indications.  This will in particular
 * wrap messages that are only written when wrapping is necessary, such as
 * Binding Responses used with ICE.  
 */
public class TurnStunProtocolEncoder implements DemuxableProtocolEncoder
    {

    private final Logger m_log = LoggerFactory.getLogger(getClass());

    public void dispose(final IoSession session) throws Exception
        {

        }

    public void encode(final IoSession session, final Object msg,
        final ProtocolEncoderOutput out) throws Exception
        {
        m_log.debug("Encoding TURN/STUN message: {}", msg);
        final StunMessage stunMessage = (StunMessage) msg;
        final TurnStunMessageMapper mapper =
            (TurnStunMessageMapper) session.getAttribute(
                "REMOTE_ADDRESS_MAP");
        final StunMessageVisitor<ByteBuffer> visitor = 
            new SendIndicationStunMessageVisitor(out, mapper);
        stunMessage.accept(visitor);
        }
    
    private final class SendIndicationStunMessageVisitor
        implements StunMessageVisitor<ByteBuffer>
        {
        
        private final ProtocolEncoderOutput m_out;
        private final TurnStunMessageMapper m_mapper;

        private SendIndicationStunMessageVisitor(
            final ProtocolEncoderOutput out, 
            final TurnStunMessageMapper mapper)
            {
            m_out = out;
            m_mapper = mapper;
            }

        private void wrapInSendIndication(final StunMessage msg)
            {
            final StunMessageEncoder encoder = new StunMessageEncoder();
            final ByteBuffer buf = encoder.encode(msg);
            
            final InetSocketAddress remoteAddress = m_mapper.get(msg);
            if (remoteAddress == null)
                {
                m_log.warn("No matching transaction ID for: {} in {}", msg, 
                    m_mapper);
                return;
                }
            final byte[] bytes = MinaUtils.toByteArray(buf);
            m_log.debug("Sending TCP framed data of length: {}", bytes.length);
            final SendIndication indication = 
                new SendIndication(remoteAddress, bytes);
            final ByteBuffer indicationBuf = encoder.encode(indication);
            m_out.write(indicationBuf);
            }
        
        private void noWrap(final StunMessage msg)
            {
            final StunMessageEncoder encoder = new StunMessageEncoder();
            final ByteBuffer buf = encoder.encode(msg);
            if (buf == null)
                {
                m_log.error("Null buffer for message: {}", msg);
                }
            else
                {
                m_out.write(buf);
                }
            }

        public ByteBuffer visitAllocateRequest(final AllocateRequest request)
            {
            noWrap(request);
            return null;
            }

        public ByteBuffer visitBindingErrorResponse(
            final BindingErrorResponse response)
            {
            wrapInSendIndication(response);
            return null;
            }

        public ByteBuffer visitBindingRequest(final BindingRequest binding)
            {
            wrapInSendIndication(binding);
            return null;
            }

        public ByteBuffer visitBindingSuccessResponse(
            final BindingSuccessResponse response)
            {
            // OK, this is weird.  Because we're using TURN here, the mapped
            // address by default is the address of the TURN server.  That's
            // not right, though -- the mapped address should be the address
            // *as if from the perspective of the TURN server*, or, i.e.,
            // the remote address from the data indication.  So we need to
            // create a new response here using the remote address from the
            // data indication, overwriting the original.
            final InetSocketAddress remoteAddress = m_mapper.get(response);
            if (remoteAddress == null)
                {
                m_log.warn("No matching transaction ID for: {}", response);
                return null;
                }      
            final UUID transactionId = response.getTransactionId();
            final StunMessage turnResponse = 
                new BindingSuccessResponse(transactionId.getRawBytes(), 
                    remoteAddress);
            wrapInSendIndication(turnResponse);
            return null;
            }

        public ByteBuffer visitCanceledMessage(
            final CanceledStunMessage message)
            {
            noWrap(message);
            return null;
            }

        public ByteBuffer visitSendIndication(final SendIndication request)
            {
            // This is a weird case.  Other protocols, such as TCP framing,
            // may already wrap their data in Send Indications.
            m_log.debug("Writing send indication...");
            noWrap(request);
            return null;
            }

        public ByteBuffer visitAllocateErrorResponse(
            final AllocateErrorResponse response)
            {
            m_log.warn("Unexpected message: {}", response);
            return null;
            }
        
        public ByteBuffer visitAllocateSuccessResponse(
            final AllocateSuccessResponse response)
            {
            m_log.warn("Unexpected message: {}", response);
            return null;
            }
        
        public ByteBuffer visitConnectErrorMesssage(
            final ConnectErrorStunMessage message)
            {
            m_log.warn("Unexpected message: {}", message);
            return null;
            }

        public ByteBuffer visitConnectRequest(final ConnectRequest request)
            {
            m_log.warn("Unexpected message: {}", request);
            return null;
            }

        public ByteBuffer visitConnectionStatusIndication(
            final ConnectionStatusIndication indication)
            {
            m_log.warn("Unexpected message: {}", indication);
            return null;
            }

        public ByteBuffer visitDataIndication(final DataIndication data)
            {
            m_log.warn("Unexpected message: {}", data);
            return null;
            }

        public ByteBuffer visitNullMessage(final NullStunMessage message)
            {
            m_log.warn("Unexpected message: {}", message);
            return null;
            }
        }
    
    @Override
    public String toString()
        {
        return getClass().getSimpleName();
        }
    
    }

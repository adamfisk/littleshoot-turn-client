package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;
import java.util.Map;

import org.apache.commons.id.uuid.UUID;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
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
import org.lastbamboo.common.util.mina.DemuxableProtocolEncoder;
import org.lastbamboo.common.util.mina.MinaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encodes bytes into STUN messages with the additional step of wrapping 
 * certaing messages in TURN Send Indications.  This will in particular
 * wrap messages that are only written when wraping is necessary, such as
 * Binding Responses used with ICE.  
 */
public class TurnStunProtocolEncoder implements DemuxableProtocolEncoder
    {

    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final Map<UUID, InetSocketAddress> m_transactionIdsToRemoteAddresses;

    /**
     * Creats a new encoder.
     * 
     * @param transactionIdsToRemoteAddresses The {@link Map} of STUN message
     * transaction IDs to the associate remote addresses responses should go
     * to.
     */
    public TurnStunProtocolEncoder(
        final Map<UUID, InetSocketAddress> transactionIdsToRemoteAddresses)
        {
        m_transactionIdsToRemoteAddresses = transactionIdsToRemoteAddresses;
        }

    public void dispose(final IoSession session) throws Exception
        {

        }

    public void encode(final IoSession session, final Object msg,
        final ProtocolEncoderOutput out) throws Exception
        {
        LOG.debug("Encoding TURN/STUN message: {}", msg);
        final StunMessage stunMessage = (StunMessage) msg;
        final StunMessageVisitor<ByteBuffer> visitor = 
            new SendIndicationStunMessageVisitor(out);
        stunMessage.accept(visitor);
        }
    
    private final class SendIndicationStunMessageVisitor
        implements StunMessageVisitor<ByteBuffer>
        {
        
        private final ProtocolEncoderOutput m_out;

        private SendIndicationStunMessageVisitor(
            final ProtocolEncoderOutput out)
            {
            m_out = out;
            }

        private void wrapInSendIndication(final StunMessage msg)
            {
            final StunMessageEncoder encoder = new StunMessageEncoder();
            final ByteBuffer buf = encoder.encode(msg);
            final InetSocketAddress remoteAddress = 
                m_transactionIdsToRemoteAddresses.get(
                    msg.getTransactionId());
            if (remoteAddress == null)
                {
                LOG.warn("No matching transaction ID for: {}", msg);
                return;
                }
            final byte[] bytes = MinaUtils.toByteArray(buf);
            LOG.debug("Sending TCP framed data of length: {}", bytes.length);
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
                LOG.error("Null buffer for message: {}", msg);
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
            final UUID transactionId = response.getTransactionId();
            final InetSocketAddress remoteAddress = 
                m_transactionIdsToRemoteAddresses.get(transactionId);
            if (remoteAddress == null)
                {
                LOG.warn("No matching transaction ID for: {}", response);
                return null;
                }            
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
            LOG.debug("Writing send indication...");
            noWrap(request);
            return null;
            }

        public ByteBuffer visitAllocateErrorResponse(
            final AllocateErrorResponse response)
            {
            LOG.warn("Unexpected message: {}", response);
            return null;
            }
        
        public ByteBuffer visitAllocateSuccessResponse(AllocateSuccessResponse response)
            {
            LOG.warn("Unexpected message: {}", response);
            return null;
            }
        
        public ByteBuffer visitConnectErrorMesssage(ConnectErrorStunMessage message)
            {
            LOG.warn("Unexpected message: {}", message);
            return null;
            }

        public ByteBuffer visitConnectRequest(ConnectRequest request)
            {
            LOG.warn("Unexpected message: {}", request);
            return null;
            }

        public ByteBuffer visitConnectionStatusIndication(
            final ConnectionStatusIndication indication)
            {
            LOG.warn("Unexpected message: {}", indication);
            return null;
            }

        public ByteBuffer visitDataIndication(final DataIndication data)
            {
            LOG.warn("Unexpected message: {}", data);
            return null;
            }

        public ByteBuffer visitNullMessage(final NullStunMessage message)
            {
            LOG.warn("Unexpected message: {}", message);
            return null;
            }
        }
    
    @Override
    public String toString()
        {
        return getClass().getSimpleName();
        }
    
    }

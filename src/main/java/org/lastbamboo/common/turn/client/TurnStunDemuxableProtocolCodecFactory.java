package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.id.uuid.UUID;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.lastbamboo.common.stun.stack.StunDemuxableProtocolCodecFactory;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.lastbamboo.common.util.mina.DemuxableProtocolCodecFactory;
import org.lastbamboo.common.util.mina.DemuxableProtocolDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DemuxableProtocolCodecFactory} for STUN.  This is slightly 
 * specialized for TURN because we need to wrap messages in Send Indications.
 * Send Indications require a REMOTE-ADDRESS attribute, but we lose the
 * remote address from the Data Indication unless we record it, which we do
 * here.  For STUN, we do this by mapping message transaction IDs to remote 
 * addresses the transactions are intended for.
 */
public class TurnStunDemuxableProtocolCodecFactory
    implements DemuxableProtocolCodecFactory<StunMessage>
    {

    private final Logger m_log = LoggerFactory.getLogger(getClass());
    
    private final DemuxableProtocolCodecFactory<StunMessage> 
        m_stunCodecFactory = new StunDemuxableProtocolCodecFactory();
    
    public boolean canDecode(final ByteBuffer in)
        {
        return this.m_stunCodecFactory.canDecode(in);
        }
    
    public boolean enoughData(final ByteBuffer in)
        {
        return this.m_stunCodecFactory.enoughData(in);
        }

    public Class<StunMessage> getClassToEncode()
        {
        return this.m_stunCodecFactory.getClassToEncode();
        }

    public DemuxableProtocolDecoder newDecoder()
        {
        return this.m_stunCodecFactory.newDecoder();
        }

    public ProtocolEncoder newEncoder()
        {
        return new TurnStunProtocolEncoder();
        }

    /*
    public void mapMessage(final StunMessage message, 
        final InetSocketAddress remoteAddress)
        {
        m_mapper.mapMessage(message, remoteAddress);
        }

    public Map<UUID, InetSocketAddress> getRawMap()
        {
        return m_mapper.getRawMap();
        }
    */

    }

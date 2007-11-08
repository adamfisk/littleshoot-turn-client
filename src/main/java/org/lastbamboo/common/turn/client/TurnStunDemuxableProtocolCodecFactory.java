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
    implements DemuxableProtocolCodecFactory<StunMessage>, TurnStunMessageMapper
    {

    private final Logger m_log = LoggerFactory.getLogger(getClass());
    
    private final DemuxableProtocolCodecFactory<StunMessage> 
        m_stunCodecFactory = new StunDemuxableProtocolCodecFactory();
    
    private final Map<UUID, InetSocketAddress> m_transactionIdsToRemoteAddresses =
        Collections.synchronizedMap(
            new LinkedHashMap<UUID, InetSocketAddress>()
                {
                protected boolean removeEldestEntry(Map.Entry eldest) 
                    {
                    // This makes the map automatically lose the least used
                    // entry.
                    return size() > 300;
                    }
                });
    
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
        return new TurnStunProtocolEncoder(m_transactionIdsToRemoteAddresses);
        }

    public void mapMessage(final StunMessage message, 
        final InetSocketAddress remoteAddress)
        {
        final UUID id = message.getTransactionId();
        if (m_transactionIdsToRemoteAddresses.containsKey(id))
            {
            m_log.warn("ID already in map: {}", id);
            return;
            }
        m_transactionIdsToRemoteAddresses.put(id, remoteAddress);
        }

    }

package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.id.uuid.UUID;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for TURN that maps STUN transaction IDs to remote addresses. 
 */
public class TurnStunMessageMapperImpl implements TurnStunMessageMapper
    {

    private final Logger m_log = LoggerFactory.getLogger(getClass());
    
    /**
     * Maps of STUN transaction IDs to remote addresses.  This caps the size
     * of the map to make sure we don't get OOMEs.
     */
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

    public InetSocketAddress get(final StunMessage msg)
        {
        return m_transactionIdsToRemoteAddresses.get(msg.getTransactionId());
        }

    }

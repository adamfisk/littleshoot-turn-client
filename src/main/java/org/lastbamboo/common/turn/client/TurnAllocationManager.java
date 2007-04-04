package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;

/**
 * This class is responsible for maintaining binding allocations with the TURN
 * servers.
 */
public interface TurnAllocationManager
    {
    /**
     * Attempts to allocate a given address.
     *
     * @param address
     *      The address to attempt to allocate.
     * @param listener
     *      The listener to be notified of connection activity.
     */
    void allocate
            (InetSocketAddress address,
             TurnServerListener listener);
    }

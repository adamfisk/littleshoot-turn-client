package org.lastbamboo.common.turn.client;

import java.util.Collection;

/**
 * Facade interface for services available to a TURN client.
 */
public interface TurnClient
    {
    /**
     * Starts the process of the TURN client connecting to servers and
     * obtaining addresses.
     */
    void start();

    /**
     * Returns the <code>Collection</code> of TURN server writers.  These are
     * servers we have allocated bindings with.
     *
     * @return The <code>Collection</code> of TURN server writers.
     */
    Collection getTurnServers();
    }

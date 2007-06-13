package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;

/**
 * Interface for the local TURN client. 
 */
public interface TurnClient
    {

    /**
     * Tells the client to connect to its TURN server.
     */
    void connect();

    /**
     * Accesses the address of the TURN server.
     * 
     * @return The address of the TURN server.
     */
    InetSocketAddress getTurnServerAddress();

    }

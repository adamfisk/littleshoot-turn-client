package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;

import org.lastbamboo.common.util.ConnectionMaintainerListener;

/**
 * Interface for the local TURN client. 
 */
public interface TurnClient
    {

    /**
     * Tells the client to connect to its TURN server.
     * 
     * @param serverAddress The address of the server to connect to. 
     * @param listener The listener for connection status to maintain 
     * connections. 
     */
    void connect(ConnectionMaintainerListener<InetSocketAddress> listener, 
        InetSocketAddress serverAddress);

    /**
     * Accesses the address of the TURN server.
     * 
     * @return The address of the TURN server.
     */
    InetSocketAddress getTurnServerAddress();

    }

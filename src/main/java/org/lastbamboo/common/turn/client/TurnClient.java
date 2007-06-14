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
     * Accesses the allocated address for this TURN client on the TURN server.
     * 
     * @return The address the TURN server allocated on this client's behalf.
     */
    InetSocketAddress getAllocatedAddress();

    /**
     * Tells the client to send a Connect Request for the specified remote
     * address, telling the TURN server to attempt to connect to the specified
     * address and to allow incoming connections from that address.
     * 
     * @param remoteAddress The address of the remote host.
     */
    void connectToRemoteHost(InetSocketAddress remoteAddress);

    }

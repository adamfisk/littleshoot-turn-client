package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;

/**
 * Interface for classes that wish to listen for the creation of TURN client
 * sockets.
 */
public interface TurnClientSocketListener
    {

    /**
     * Called when a TURN client socket is created.
     * 
     * @param remoteHost The remote host this socket ultimately exchanges data
     * with, via the TURN server.
     * @param candidate The created <code>Socket</code>.
     */
    void onTurnClientSocketCreated(final InetSocketAddress remoteHost, 
        final TurnClientSocketCandidate candidate);

    }

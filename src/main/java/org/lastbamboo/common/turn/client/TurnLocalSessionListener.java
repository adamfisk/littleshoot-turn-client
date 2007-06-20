package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;

import org.apache.mina.common.IoSession;

/**
 * Listener for events on one of the local connections from the TURN client
 * to the HTTP server.  Remember, there's one local connection from the TURN
 * client to the HTTP server for *every remote host we're exchanging data with*.
 */
public interface TurnLocalSessionListener
    {

    /**
     * Called when a local session is closed.
     * 
     * @param remoteAddress The remote address this is a local session on 
     * behalf of.
     * @param session The session that closed.
     */
    void onLocalSessionClosed(InetSocketAddress remoteAddress, 
        IoSession session);

    }

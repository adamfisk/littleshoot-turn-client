package org.lastbamboo.common.turn.client;

import java.net.Socket;

/**
 * Interface for a single candidate socket for connecting with a remote host.
 * This provides access to the socket and allows other classes to determine 
 * whether or not this socket is already being used by another socket 
 * consumer.
 */
public interface TurnClientSocketCandidate
    {

    /**
     * Provides access to the socket.
     * @return The socket.
     */
    Socket getSocket();

    }

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
    
    /**
     * Returns whether or not another socket consumer is already using this
     * socket.  In general, only one socket consumer should have access to
     * a socket at a time.  A web server should not have access to a socket
     * while a TURN message writer is also using it, for example.
     * @return
     */
    boolean isTaken();
    
    /**
     * Sets whether or not a socket consumer has claimed this socket.
     * @param taken <code>true</code> if a socket consumer has laid 
     * exclusive claim to this socket, otherwise <code>false</code>.
     */
    void setTaken(final boolean taken);
    }

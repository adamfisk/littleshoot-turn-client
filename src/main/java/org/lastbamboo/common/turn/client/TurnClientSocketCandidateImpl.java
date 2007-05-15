package org.lastbamboo.common.turn.client;

import java.net.Socket;

/**
 * Wrapper class for an individual TURN client socket.  This provides access
 * to the socket and keeps track of whether or not the socket is already in
 * use.
 */
public final class TurnClientSocketCandidateImpl 
    implements TurnClientSocketCandidate
    {

    private final Socket m_socket;

    /**
     * Creates a new candidate wrapper for the specified socket.
     * @param sock The socket to wrap.
     */
    public TurnClientSocketCandidateImpl(final Socket sock)
        {
        this.m_socket = sock;
        }
    
    public Socket getSocket()
        {
        return this.m_socket;
        }
    }

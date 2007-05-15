package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;
import java.util.Collection;

/**
 * Interface for classes that track and maintain bindings on a set of 
 * TURN servers.
 */
public interface TurnServerTracker
    {

    /**
     * Adds a server at the specified address and port to the active TURN server
     * group.  This method should only be called if we've received a TURN
     * ALLOCATE-RESPONSE method from this server.
     * @param server The TURN server to add.
     */
    void addTurnServer(final TurnServerWriter server);
    
    /**
     * Removes the specified address from the group of TURN of active TURN 
     * servers.  This should be called if we lose the connection to the server,
     * for example.
     * @param server The TURN server to remove.
     */
    void removeTurnServer(final TurnServerWriter server);
    
    /**
     * Adds a server at the specified address and port to the candidate servers
     * to connect to.
     * @param server The IP address and port for the server.
     */
    void addCandidateTurnServer(final InetSocketAddress server);
    
    /**
     * Adds the specified <code>Collection</code> of TURN servers to the 
     * candidate servers to use.
     * @param servers The addresses of TURN servers to use if 
     * necessary.  These are <code>InetSocketAddress</code>es.
     */
    void addCandidateTurnServers(final Collection<InetSocketAddress> servers);
    
    /**
     * Removes the specified address from the group of TURN servers to connect
     * to.
     * @param server The IP address and port of the server to remove.
     */
    void removeCandidateTurnServer(final InetSocketAddress server);
    
    /**
     * Accessor for the currently active TURN servers.  These are the servers 
     * we currently have allocated addresses on, i.e. we have received an 
     * ALLOCATE-RESPONSE message from these servers.  This returns a copy of 
     * the <code>Collection</code> of servers, allowing callers to disregard 
     * threading issues.
     * 
     * @return The <code>Collection</code> of currently active TURN servers that
     * have allocated an address on our behalf.  The returned 
     * <code>Collection</code> is a copy.
     */
    Collection getTurnServers();
    
    /**
     * Accessor for the TURN server candidates.  These are 
     * <code>InetSocketAddress</code>es for TURN servers we can try to 
     * connect to.
     * @return The <code>Collection</code> of <code>InetSocketAddress</code>es
     * for TURN servers to connect to.
     */
    Collection getCandidateTurnServers();
    }

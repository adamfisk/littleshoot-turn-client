package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Maintains bindings on a set of TURN servers, keeping track of binding 
 * timeouts, available TURN servers, the number of server connections desired,
 * etc.
 */
public final class TurnServerTrackerImpl implements TurnServerTracker
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG = 
        LogFactory.getLog(TurnServerTrackerImpl.class);
    
    /**
     * <code>Set</code> pf <code>TurnServerWriter</code>s for TURN servers 
     * we are currently connected to.
     */
    private final Set m_turnServers = 
        Collections.synchronizedSet(new HashSet());
    
    /**
     * <code>Collection</code> of <code>InetSocketAddress</code>es for TURN
     * servers we could potentially connect to.
     */
    private final Set m_turnServerCandidates = 
        Collections.synchronizedSet(new HashSet());
    
    public void addTurnServer(final TurnServerWriter server)
        {
        LOG.trace("Adding TURN server: "+server);
        this.m_turnServers.add(server);
        }

    public void addTurnServers(final Collection servers)
        {
        LOG.trace("Adding "+servers.size()+" servers...");
        synchronized(this.m_turnServers)
            {
            this.m_turnServers.addAll(servers);
            }
        }

    public void removeTurnServer(final TurnServerWriter server)
        {
        LOG.trace("Removing TURN server: "+server);
        this.m_turnServers.remove(server);
        }

    public void addCandidateTurnServer(final InetSocketAddress server)
        {
        LOG.trace("Adding candidate TURN server: " + server);
        this.m_turnServerCandidates.add(server);
        }

    public void addCandidateTurnServers(final Collection servers)
        {
        synchronized(this.m_turnServerCandidates)
            {
            this.m_turnServerCandidates.addAll(servers);
            }
        }

    public void removeCandidateTurnServer(final InetSocketAddress server)
        {
        this.m_turnServerCandidates.remove(server);
        }

    public Collection getTurnServers()
        {
        return copy(this.m_turnServers, new HashSet());
        }

    public Collection getCandidateTurnServers()
        {
        return copy(this.m_turnServerCandidates, new HashSet());
        }
    
    /**
     * Utility method for creating a copy of a <code>Collection</code> so
     * callers cannot modify the internal data of this class.
     * @param original The <code>Collection</code> to make a copy of.
     * @param copy The new <code>Collection</code> that is a copy of the
     * original collection.
     * @return A copy of the original <code>Collection</code>.
     */
    private Collection copy(final Collection original, final Collection copy)
        {
        synchronized (original)
            {
            copy.addAll(original);
            }
        return Collections.unmodifiableCollection(copy);
        }


    }

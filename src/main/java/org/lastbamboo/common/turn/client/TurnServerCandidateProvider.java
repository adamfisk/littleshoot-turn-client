package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.stun.stack.StunConstants;
import org.lastbamboo.common.util.CandidateProvider;

/**
 * The candidate provider that provides candidate TURN servers.
 */
public final class TurnServerCandidateProvider implements CandidateProvider
    {
    /**
     * The log for this class.
     */
    private static final Log LOG =
        LogFactory.getLog (TurnServerCandidateProvider.class);

    /**
     * {@inheritDoc}
     */
    public Collection getCandidates()
        {
        LOG.debug("Accessing TURN servers...");
        final InetSocketAddress turnServer =
            new InetSocketAddress("lastbamboo.org", StunConstants.STUN_PORT);
        final Collection<InetSocketAddress> servers = 
            new LinkedList<InetSocketAddress>();
        
        servers.add(turnServer);
        return servers;
        }
    }
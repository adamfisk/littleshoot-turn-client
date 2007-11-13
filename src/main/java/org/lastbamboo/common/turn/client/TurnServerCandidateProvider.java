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
public final class TurnServerCandidateProvider 
    implements CandidateProvider<InetSocketAddress>
    {
    /**
     * The log for this class.
     */
    private static final Log LOG =
        LogFactory.getLog (TurnServerCandidateProvider.class);

    /**
     * {@inheritDoc}
     */
    public Collection<InetSocketAddress> getCandidates()
        {
        LOG.debug("Accessing TURN servers...");
        // TODO: We need to access the list of TURN servers available from
        // S3.  Same for SIP.
        final InetSocketAddress turnServer =
            new InetSocketAddress(
                "ec2-67-202-6-199.z-1.compute-1.amazonaws.com", 
                StunConstants.STUN_PORT);
        final Collection<InetSocketAddress> servers = 
            new LinkedList<InetSocketAddress>();
        
        servers.add(turnServer);
        return servers;
        }

    public InetSocketAddress getCandidate()
        {
        return getCandidates().iterator().next();
        }
    }

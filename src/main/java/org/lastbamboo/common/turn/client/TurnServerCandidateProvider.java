package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.http.client.HttpClientGetRequester;
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

    private static final String API_URL = 
        "http://www.lastbamboo.org/lastbamboo-server-site/api/sipServer";
    
    /**
     * {@inheritDoc}
     */
    public Collection<InetSocketAddress> getCandidates()
        {
        LOG.debug("Accessing TURN servers...");
        final InetSocketAddress turnServer = getCandidate();
        if (turnServer == null)
            {
            LOG.error("Could not access TURN servers!!!");
            return Collections.emptySet();
            }
        final Collection<InetSocketAddress> servers = 
            new LinkedList<InetSocketAddress>();
        
        servers.add(turnServer);
        return servers;
        }

    public InetSocketAddress getCandidate()
        {
        final HttpClientGetRequester requester = 
            new HttpClientGetRequester();
        final String data = requester.request(API_URL);
        if (StringUtils.isBlank(data) || !data.contains(":"))
            {
            LOG.error("Bad data from server: " + data);
            return null;
            }
        final String host = StringUtils.substringBefore(data, ":");

        // Note we ignore the port and just use the default TURN port.
        return new InetSocketAddress(host, StunConstants.STUN_PORT);
        }
    }

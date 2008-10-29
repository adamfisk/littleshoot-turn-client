package org.lastbamboo.common.turn.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.http.client.HttpClientGetRequester;
import org.lastbamboo.common.http.client.ServiceUnavailableException;
import org.lastbamboo.common.json.JsonUtils;
import org.lastbamboo.common.util.CandidateProvider;
import org.lastbamboo.common.util.ShootConstants;

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
        ShootConstants.SERVER_URL+"/api/turnServer";
    
    public Collection<InetSocketAddress> getCandidates()
        {
        LOG.debug("Accessing TURN servers...");
        final String data = getData();
        if (StringUtils.isBlank(data))
            {
            LOG.error("Bad data from server: " + data);
            return Collections.emptySet();
            }
        return JsonUtils.getInetAddresses(data);
        }
    
    public InetSocketAddress getCandidate()
        {
        final Collection<InetSocketAddress> candidates = getCandidates();
        if (candidates.isEmpty()) return null;
        return candidates.iterator().next();
        }

    private String getData()
        {
        final HttpClientGetRequester requester = new HttpClientGetRequester();
        final String data;
        try
            {
            // Note this will automatically decompress the body if necessary.
            data = requester.request(API_URL);
            }
        catch (final IOException e)
            {
            LOG.error("Could not access TURN server data", e);
            return null;
            }
        catch (final ServiceUnavailableException e)
            {
            LOG.error("Could not access TURN server data", e);
            return null;
            }
        return data;
        }
    }

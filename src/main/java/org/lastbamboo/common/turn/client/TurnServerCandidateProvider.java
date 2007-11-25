package org.lastbamboo.common.turn.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lastbamboo.common.http.client.HttpClientGetRequester;
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
        "http://www.lastbamboo.org/lastbamboo-server-site/api/turnServer";
    
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
        return getSocketAddress();
        }
    
    private static InetSocketAddress getSocketAddress()
        {
        final HttpClientGetRequester requester = 
            new HttpClientGetRequester();
        final String data;
        try
            {
            // Note this will automatically decompress the body if necessary.
            data = requester.request(API_URL);
            }
        catch (final IOException e)
            {
            LOG.error("Could not access SIP server data");
            return null;
            }
        if (StringUtils.isBlank(data))
            {
            LOG.error("Bad data from server: " + data);
            return null;
            }
        
        try
            {
            final JSONObject json = new JSONObject(data);
            final JSONArray servers = json.getJSONArray("servers");
            final JSONObject server = servers.getJSONObject(0);
            return new InetSocketAddress(server.getString("address"), 
                server.getInt("port"));
            }
        catch (final JSONException e)
            {
            LOG.error("Could not read JSON: "+data, e);
            return null;
            }
        }
    }

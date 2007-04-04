package org.lastbamboo.common.turn.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Loads TURN server from a default file on disk.
 */
public final class TurnServerLoaderImpl implements TurnServerLoader
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(TurnServerLoaderImpl.class);

    private TurnServerTracker m_turnServerTracker;
    private URL m_serversUrl;

    /**
     * Set the TURN server tracker to notify of servers we've loaded.
     * @param tracker The TURN server tracker to notify.
     */
    public void setTurnServerTracker(final TurnServerTracker tracker)
        {
        this.m_turnServerTracker = tracker;
        }

    public void loadServers(final int defaultPort)
        {
        try
            {
            final BufferedReader reader = createReader();
            loadServers(reader, defaultPort);
            }
        catch (final IOException e)
            {
            LOG.warn("Unexpected exception", e);
            }
        }

    /**
     * Creates the <code>BufferedReader</code> for the TURN server file on disk.
     * @return The <code>BufferedReader</code> for the TURN server file.
     * @throws IOException If the file could not be located.
     */
    private BufferedReader createReader() throws IOException
        {
        final InputStream servers = this.m_serversUrl.openStream();
        final Reader fileReader = new InputStreamReader(servers);
        return new BufferedReader(fileReader);
        }

    /**
     * Loads the servers from the given <code>BufferedReader</code>.
     * @param reader The reader to load the servers from.
     * @throws IOException If there's a read error reading the server file.
     */
    private void loadServers(final BufferedReader reader,
                             final int defaultPort) throws IOException
        {
        String curServer = reader.readLine().trim();
        while (curServer != null)
            {
            LOG.trace("Loading TURN server: "+curServer);
            final InetAddress address = InetAddress.getByName(curServer);
            final InetSocketAddress server =
                new InetSocketAddress(address, defaultPort);

            // Store the server in the tracker.
            this.m_turnServerTracker.addCandidateTurnServer(server);
            curServer = reader.readLine();
            }
        }

    public void setServersUrl(final URL servers)
        {
        this.m_serversUrl = servers;
        }

    }

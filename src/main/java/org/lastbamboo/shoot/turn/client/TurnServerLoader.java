package org.lastbamboo.shoot.turn.client;

import java.net.URL;


/**
 * Interface for classes that can load TURN server address data.
 */
public interface TurnServerLoader
    {

    /**
     * Loads server data from the default turn-servers.properties file
     * on disk.
     *
     * @param defaultPort
     *      The default port on which to connect to the servers.
     */
    void loadServers(int defaultPort);

    /**
     * The <code>URL</code> of the file containing TURN servers to use.
     * @param servers The <code>URL</code> of the file containing TURN
     * servers to use.
     */
    void setServersUrl(final URL servers);
    }

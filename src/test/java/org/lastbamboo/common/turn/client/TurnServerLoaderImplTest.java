package org.lastbamboo.common.turn.client;

import java.io.File;
import java.io.FileWriter;
import java.util.Collection;

import org.lastbamboo.common.turn.TurnConstants;
import org.lastbamboo.common.turn.client.TurnServerLoaderImpl;
import org.lastbamboo.common.turn.client.TurnServerTracker;
import org.lastbamboo.common.turn.client.TurnServerTrackerImpl;

import junit.framework.TestCase;

/**
 * Tests the class for loading TURN servers from disk.
 */
public class TurnServerLoaderImplTest extends TestCase
    {

    /*
     * Test method for
     * 'org.lastbamboo.shoot.turn.client.TurnServerLoaderImpl.loadServers()'
     */
    public void testLoadServers() throws Exception
        {
        final File turnServerFile = new File(TurnConstants.SERVER_FILE_NAME);
        turnServerFile.delete();
        turnServerFile.deleteOnExit();
        final FileWriter writer = new FileWriter(turnServerFile);
        writer.write("127.0.0.1\r\n");
        writer.close();

        final TurnServerTracker tracker = new TurnServerTrackerImpl();
        Collection servers = tracker.getCandidateTurnServers();
        assertEquals(0, servers.size());
        final TurnServerLoaderImpl loader = new TurnServerLoaderImpl();
        loader.setTurnServerTracker(tracker);
        loader.setServersUrl(turnServerFile.toURL());
        loader.loadServers(TurnConstants.DEFAULT_SERVER_PORT);

        servers = tracker.getCandidateTurnServers();
        assertEquals(1, servers.size());
        }

    }

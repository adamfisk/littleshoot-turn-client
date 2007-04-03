package org.lastbamboo.shoot.turn.client;

import java.net.InetSocketAddress;

import junit.framework.TestCase;

import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.shoot.turn.client.stub.ReaderWriterStub;

/**
 * Tests the class for keeping track of remote sockets.
 */
public final class TurnReaderWriterTrackerImplTest extends TestCase
    {

    /**
     * Tests the method for checking whether or not a socket exists.
     * @throws Exception If any unexpected error occurs.
     */
    public void testHasReaderWriter() throws Exception
        {
        final TurnReaderWriterTrackerImpl tracker = new TurnReaderWriterTrackerImpl();
        final InetSocketAddress remoteAddress =
            new InetSocketAddress("127.0.0.1", 7893);
        final ReaderWriter readerWriter = new ReaderWriterStub();
        
        assertFalse(tracker.hasReaderWriter(remoteAddress));
        tracker.addReaderWriter(remoteAddress, readerWriter);
        assertTrue(tracker.hasReaderWriter(remoteAddress));
        }

    /**
     * Tests the method for adding a socket.
     * @throws Exception If any unexpected error occurs.
     */
    public void testAddReaderWriter()
        {
        final TurnReaderWriterTrackerImpl tracker = 
            new TurnReaderWriterTrackerImpl();
        final InetSocketAddress remoteAddress =
            new InetSocketAddress("127.0.0.1", 7893);        
        final ReaderWriter readerWriter = new ReaderWriterStub();
        
        assertFalse(tracker.hasReaderWriter(remoteAddress));
        tracker.addReaderWriter(remoteAddress, readerWriter);
        assertTrue(tracker.hasReaderWriter(remoteAddress));
        }

    /**
     * Tests the method for accessing an existing socket.
     * @throws Exception If any unexpected error occurs.
     */
    public void testGetReaderWriter()
        {
        final TurnReaderWriterTrackerImpl tracker = new TurnReaderWriterTrackerImpl();
        final InetSocketAddress remoteAddress =
            new InetSocketAddress("127.0.0.1", 7893);
        final ReaderWriter readerWriter = new ReaderWriterStub();
        
        assertFalse(tracker.hasReaderWriter(remoteAddress));
        tracker.addReaderWriter(remoteAddress, readerWriter);
        assertTrue(tracker.hasReaderWriter(remoteAddress));
        
        final ReaderWriter storedReaderWriter = 
            tracker.getReaderWriter(remoteAddress);
        
        assertEquals(readerWriter, storedReaderWriter);
        }

    /**
     * Test to make sure the onClose method properly removes reader/writers.
     * @throws Exception If any unexpected error occurs.
     */
    public void testOnClose() throws Exception
        {
        final TurnReaderWriterTrackerImpl tracker = 
            new TurnReaderWriterTrackerImpl();
        final InetSocketAddress remoteAddress =
            new InetSocketAddress("127.0.0.1", 7893);        
        final ReaderWriter readerWriter = new ReaderWriterStub();
        
        assertFalse(tracker.hasReaderWriter(remoteAddress));
        tracker.addReaderWriter(remoteAddress, readerWriter);
        assertTrue(tracker.hasReaderWriter(remoteAddress));
        
        // Now make sure the on close event removes the reader/writer.
        tracker.onClose(readerWriter);
        assertFalse(tracker.hasReaderWriter(remoteAddress));
        }
    }

package org.lastbamboo.shoot.turn.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import junit.framework.TestCase;

import org.easymock.MockControl;

import org.lastbamboo.shoot.protocol.ReaderWriter;
import org.lastbamboo.shoot.turn.client.stub.ReaderWriterStub;
import org.lastbamboo.shoot.turn.message.TurnMessageFactory;
import org.lastbamboo.shoot.turn.message.TurnMessageFactoryImpl;

/**
 * Tests the class for writing messages to TURN servers.
 */
public final class TurnServerWriterImplTest extends TestCase
    {

    /**
     * Tests the send method to make sure it waits appropriately for a response.
     *
     * @throws Exception If any unexpected error occurs.
     */
    public void testWriteSendRequest() throws Exception
        {
        final ReaderWriter readerWriter = new ReaderWriterStub();

        final MockControl turnServerListenerControl =
                MockControl.createControl (TurnServerListener.class);

        final TurnServerListener turnServerListener =
                (TurnServerListener) turnServerListenerControl.getMock ();

        final TurnMessageFactory messageFactory = new TurnMessageFactoryImpl();
        final InetSocketAddress mappedAddress =
            new InetSocketAddress(InetAddress.getLocalHost(), 4322);

        final TurnReaderWriterTracker readerWriterTracker =
            new TurnReaderWriterTrackerImpl();
        final TurnServerWriterImpl server =
            new TurnServerWriterImpl(readerWriter, turnServerListener,
                messageFactory, mappedAddress, readerWriterTracker);
        }

    }

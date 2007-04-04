package org.lastbamboo.common.turn.client;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.easymock.MockControl;

import org.lastbamboo.common.nio.NioServerImpl;
import org.lastbamboo.common.nio.SelectorManagerImpl;
import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.common.turn.client.DataIndicationListener;
import org.lastbamboo.common.turn.client.DataIndicationListenerImpl;
import org.lastbamboo.common.turn.client.TurnReaderWriterFactoryImpl;
import org.lastbamboo.common.turn.client.TurnReaderWriterTracker;
import org.lastbamboo.common.turn.client.TurnReaderWriterTrackerImpl;
import org.lastbamboo.common.turn.client.TurnServerListener;
import org.lastbamboo.common.turn.client.TurnServerWriter;
import org.lastbamboo.common.turn.client.TurnServerWriterImpl;
import org.lastbamboo.common.turn.client.stub.ReaderWriterStub;
import org.lastbamboo.common.turn.message.DataIndication;
import org.lastbamboo.common.turn.message.DataIndicationImpl;
import org.lastbamboo.common.turn.message.TurnMessageFactoryImpl;
import org.lastbamboo.common.turn.message.attribute.TurnAttributeFactory;
import org.lastbamboo.common.turn.message.attribute.TurnAttributeFactoryImpl;
import org.lastbamboo.util.FuncWithReturn;
import org.lastbamboo.util.SocketHandler;

/**
 * Test for the class that listens for TURN "Data Indication" messages.
 */
public final class DataIndicationListenerImplTest extends TestCase
    implements SocketHandler
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG =
        LogFactory.getLog(DataIndicationListenerImplTest.class);

    private InetSocketAddress m_remoteHost;
    private Socket m_socket;

    protected void setUp() throws Exception
        {
        this.m_remoteHost = null;
        this.m_socket = null;
        }

    /**
     * Tests the method for handling data indication messages.
     */
    public void testOnDataIndication() throws Exception
        {
        final InetSocketAddress remoteAddress =
            new InetSocketAddress("124.5.67.5", 4388);
        final TurnReaderWriterTracker tracker =
            new TurnReaderWriterTrackerImpl();
        final SelectorManagerImpl acceptSelector = new SelectorManagerImpl();

        final TurnReaderWriterFactoryImpl factory =
            new TurnReaderWriterFactoryImpl(acceptSelector);

        final int localPort = 7678; // TODO
        final NioServerImpl server =
            new NioServerImpl(localPort, acceptSelector, factory);
        server.startServer();
        
        final FuncWithReturn<Integer> portProvider =
                new FuncWithReturn<Integer>()
            { 
            public Integer run
                    ()
                {
                return localPort;
                }
            };

        final DataIndicationListener dataListener =
                new DataIndicationListenerImpl(tracker, factory, this,
                                               portProvider);

        final byte[] dataBytes = new byte[10];

        for (int i = 0; i < dataBytes.length; i++)
            {
            dataBytes[i] = (byte) i;
            }
        final ByteBuffer dataBuffer = ByteBuffer.wrap(dataBytes);

        final TurnAttributeFactory attributeFactory =
            new TurnAttributeFactoryImpl();

        final TurnMessageFactoryImpl messageFactory =
            new TurnMessageFactoryImpl();

        messageFactory.setAttributeFactory(attributeFactory);

        final DataIndication data =
            new DataIndicationImpl(attributeFactory, dataBuffer, remoteAddress);

        final ReaderWriter readerWriter = new ReaderWriterStub();

        final MockControl turnServerListenerControl =
                MockControl.createControl (TurnServerListener.class);

        final TurnServerListener turnServerListener =
                (TurnServerListener) turnServerListenerControl.getMock ();

        final InetSocketAddress mappedAddress =
                new InetSocketAddress("43.4.57.7", 2432);

        final TurnServerWriter turnWriter =
                new TurnServerWriterImpl (readerWriter, turnServerListener,
                                          messageFactory, mappedAddress,
                                          new TurnReaderWriterTrackerImpl());

        assertNull("Remote host should be null", this.m_remoteHost);
        dataListener.onDataIndication(turnWriter, data);

        int counter = 0;
        while (counter < 10 && !tracker.hasReaderWriter(remoteAddress))
            {
            Thread.sleep(400);
            counter++;
            }

        assertTrue(tracker.hasReaderWriter(remoteAddress));
        assertNotNull(tracker.getReaderWriter(remoteAddress));

        //synchronized (this)
          //  {
            //while (this.m_remoteHost == null)
              //  {
                //wait(6000);
                //break;
                //}
            //}

        //assertNotNull("Listener did not get remote address", this.m_remoteHost);
        //assertEquals(remoteAddress, this.m_remoteHost);
        assertNotNull("Listener did not get client socket", this.m_socket);

        final InputStream is = this.m_socket.getInputStream();

        final byte[] dataBytesReceived = new byte[dataBytes.length];
        assertFalse(Arrays.equals(dataBytes, dataBytesReceived));

        int read = is.read(dataBytesReceived);

        int count = 0;
        while (read < dataBytes.length && count < 20)
            {
            read += is.read(dataBytesReceived);
            count++;
            }

        //The socket in ReaderWriterFactoryImpl is to the server on the accept
        //thread.  Does this mean we won't see the appropriate data because we're
        //writing to the wrong socket?  I'm not sure here.  I guess it should
        //only really matter what selector you're registered with??
        assertEquals("Did not read expected # bytes", dataBytes.length, read);
        assertTrue(Arrays.equals(dataBytes, dataBytesReceived));
        }


    public void handleSocket(final Socket socket)
        {
        LOG.trace("Handling socket...");
        this.m_socket = socket;
        try
            {
            this.m_socket.setSoTimeout(4000);
            }
        catch (final SocketException e)
            {
            LOG.error("Should not happen", e);
            }
        synchronized (this)
            {
            notifyAll();
            }
        }
    }

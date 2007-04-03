package org.lastbamboo.shoot.turn.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.easymock.MockControl;
import org.lastbamboo.common.nio.NioServerImpl;
import org.lastbamboo.common.nio.SelectorManager;
import org.lastbamboo.common.nio.SelectorManagerImpl;
import org.lastbamboo.common.protocol.ProtocolHandler;
import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.shoot.turn.client.stub.ReaderWriterStub;
import org.lastbamboo.shoot.turn.message.TurnMessageFactoryImpl;
import org.lastbamboo.shoot.turn.message.attribute.TurnAttributeFactoryImpl;
import org.lastbamboo.util.SocketHandler;

/**
 * Tests the class for creating TURN sockets using the local server proxy.
 */
public class TurnReaderWriterFactoryImplTest extends TestCase
    implements SocketHandler
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG =
        LogFactory.getLog(TurnReaderWriterFactoryImplTest.class);

    //private TurnClientSocketCandidate m_candidate;

    private static final String MESSAGE =
        "The best thing about ASIA is the cookies";

    private Socket m_socket;

    /**
     * Tests the method for creating a reader/writer when the read selector
     * thread is blocking.
     *
     * @throws Exception If any unexpecter error occurs.
     */
    public void testCreateReaderWriterWithReadThreadBlocking()
        throws Exception
        {
        this.m_socket = null;
        final InetSocketAddress destinationAddress =
            new InetSocketAddress("43.4.32.5", 8043);
        final SelectorManager acceptSelector = new SelectorManagerImpl();

        final TurnReaderWriterFactoryImpl factory =
            new TurnReaderWriterFactoryImpl(acceptSelector);

        final int localPort = 7678; // TODO
        final NioServerImpl server =
            new NioServerImpl(localPort, acceptSelector, factory);
        server.startServer();

//        final MockControl socketHandlerControl =
//            MockControl.createControl(SocketHandler.class);
//        final SocketHandler socketHandler =
//            (SocketHandler) socketHandlerControl.getMock();
//        socketHandler.handleSocket(null);
//        socketHandlerControl.setMatcher(MockControl.ALWAYS_MATCHER);
//        socketHandlerControl.setVoidCallable(1);
//        socketHandlerControl.replay();

        final TurnServerWriter turnWriter = createTurnServerWriter();

        //this.m_candidate = null;
        final ReaderWriter readerWriter =
            factory.createReaderWriter(destinationAddress, turnWriter, this,
                                       localPort);

        LOG.trace("Created first reader/writer*********************....");
        assertNotNull("Should have created a socket", readerWriter);
        assertNotNull("Should have accepted a socket", this.m_socket);


        // Set the protocol handler to not block.
        readerWriter.setProtocolHandler(new NonBlockingProtocolHandler());
        //final Socket socket = this.m_candidate.getSocket();

        LOG.trace("Writing message...");
        this.m_socket.getOutputStream().write(MESSAGE.getBytes());

        LOG.trace("Should be creating reader/writer with selector: "+
            acceptSelector);
        final ReaderWriter readerWriter2 =
            factory.createReaderWriter(destinationAddress, turnWriter, this,
                                       localPort);

        assertNotNull(readerWriter2);

        LOG.trace("Closing server...");
        server.close();
        Thread.sleep(2000);
        }

    private static final class NonBlockingProtocolHandler
        implements ProtocolHandler
        {

        public void handleMessages(final ByteBuffer buffer,
            final InetSocketAddress remoteHost)
            {
            buffer.flip();
            final byte[] message = new byte[MESSAGE.length()];
            buffer.get(message);
            LOG.trace("Received message: "+new String(message));
            }

        }

    /**
     * Tests the socket creation method.
     * @throws Exception If any unexpected error occurs.
     */
    public void testCreateReaderWriter() throws Exception
        {
        final InetSocketAddress destinationAddress =
            new InetSocketAddress("43.4.32.5", 8043);
        final SelectorManager acceptSelector = new SelectorManagerImpl();

        /*
        final MockControl clientSocketListenerControl =
            MockControl.createControl(TurnClientSocketListener.class);
        final TurnClientSocketListener clientSocketListener =
            (TurnClientSocketListener) clientSocketListenerControl.getMock();
        clientSocketListener.onTurnClientSocketCreated(destinationAddress,
            new TurnClientSocketCandidateImpl(new Socket()));
        clientSocketListenerControl.setMatcher(MockControl.ALWAYS_MATCHER);
        clientSocketListenerControl.replay();
        */

        final TurnReaderWriterFactoryImpl factory =
            new TurnReaderWriterFactoryImpl(acceptSelector);

        final int localPort = 7678; // TODO
        final NioServerImpl server =
            new NioServerImpl(localPort, acceptSelector, factory);
        server.startServer();

        final TurnServerWriter turnWriter = createTurnServerWriter();

        LOG.trace("Creating reader/writer...");
        this.m_socket = null;
        final ReaderWriter readerWriter =
            factory.createReaderWriter(destinationAddress, turnWriter, this,
                                       localPort);

        assertNotNull("Should have created a socket", readerWriter);
        assertNotNull(this.m_socket);

        //clientSocketListenerControl.verify();
        server.close();
        }

    private TurnServerWriter createTurnServerWriter()
        {
        final TurnAttributeFactoryImpl attributeFactory =
            new TurnAttributeFactoryImpl();

        final TurnMessageFactoryImpl messageFactory =
            new TurnMessageFactoryImpl();
        messageFactory.setAttributeFactory(attributeFactory);

        final MockControl turnServerListenerControl =
                MockControl.createControl (TurnServerListener.class);

        final TurnServerListener turnServerListener =
                (TurnServerListener) turnServerListenerControl.getMock ();

        final InetSocketAddress mappedAddress =
            new InetSocketAddress("43.4.57.7", 2432);

        return new TurnServerWriterImpl (new ReaderWriterStub (),
                                         turnServerListener, messageFactory,
                                         mappedAddress,
                                         new TurnReaderWriterTrackerImpl ());
        }

    /*
    public void onTurnClientSocketCreated(final InetSocketAddress remoteHost,
        final TurnClientSocketCandidate candidate)
        {
        this.m_candidate = candidate;
        }
        */

    public void handleSocket(final Socket socket)
        {
        this.m_socket = socket;
        }

    }

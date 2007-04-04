package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.easymock.MockControl;
import org.lastbamboo.common.turn.client.TurnReaderWriterTrackerImpl;
import org.lastbamboo.common.turn.client.TurnServerListener;
import org.lastbamboo.common.turn.client.TurnServerWriter;
import org.lastbamboo.common.turn.client.TurnServerWriterImpl;
import org.lastbamboo.common.turn.client.stub.ReaderWriterStub;
import org.lastbamboo.shoot.turn.message.TurnMessageFactoryImpl;
import org.lastbamboo.shoot.turn.message.attribute.TurnAttributeFactoryImpl;
import org.lastbamboo.util.SocketHandler;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Test to make sure the local TURN client proxy server starts successfully
 * from Spring config files.
 */
public class LocalProxyTest
    extends AbstractDependencyInjectionSpringContextTests
    implements SocketHandler
    {

    protected String[] getConfigLocations()
        {
        return new String[] {"turnStackBeans.xml"};
        //return new String[] {"turnApplicationContext.xml",
          //  "turnClientApplicationContext.xml",
            //"httpApplicationContext.xml"};
        }

    /**
     * Just tests that we can hit the local proxy from the proxy started
     * in Spring config files.
     * @throws Exception If any unexpected error occurs.
     */
    public void testHitLocalProxy() throws Exception
        {
        /*
        final TurnReaderWriterFactory socketFactory =
            (TurnReaderWriterFactory) applicationContext.getBean(
                "turnReaderWriterFactory");

        final InetSocketAddress destinationAddress =
            new InetSocketAddress("43.4.32.5", 8043);
        final ReaderWriter readerWriter =
            socketFactory.createReaderWriter(destinationAddress,
                createTurnServerWriter(), this);

        assertNotNull("Could not get reader writer", readerWriter);
        */
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

    public void handleSocket(final Socket socket)
        {
        // Nothing to do.
        }
    }

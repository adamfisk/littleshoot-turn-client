package org.lastbamboo.shoot.turn.client;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.protocol.ProtocolHandler;
import org.lastbamboo.common.protocol.ReadWriteConnectorListener;
import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.common.protocol.ServerConnector;
import org.lastbamboo.shoot.turn.TurnProtocolHandler;
import org.lastbamboo.shoot.turn.message.TurnMessage;
import org.lastbamboo.shoot.turn.message.TurnMessageFactory;
import org.lastbamboo.shoot.turn.message.TurnMessageVisitor;
import org.lastbamboo.shoot.turn.message.handler.TurnMessageHandlerFactory;
import org.lastbamboo.util.FuncWithReturn;
import org.lastbamboo.util.SocketHandler;

/**
 * Maintains sufficient allocated addresses with the available TURN servers.
 */
public final class TurnAllocationManagerImpl implements TurnAllocationManager
    {
    /**
     * Logger for this class.
     */
    private static final Log LOG =
            LogFactory.getLog (TurnAllocationManagerImpl.class);

    /**
     * TODO.
     */
    private final ServerConnector m_turnServerConnector;

    /**
     * TODO.
     */
    private final TurnMessageFactory m_turnMessageFactory;

    /**
     * TODO.
     */
    private final TurnMessageHandlerFactory m_turnMessageHandlerFactory;

    /**
     * TODO.
     */
    private final TurnReaderWriterFactory m_turnSocketFactory;
    
    /**
     * TODO.
     */
    private final FuncWithReturn<Integer> m_portProvider;

    /**
     * TODO.
     */
    private SocketHandler m_socketHandler;

    /**
     * TODO.
     */
    public TurnAllocationManagerImpl
            (final TurnMessageFactory messageFactory,
             final ServerConnector serverConnector,
             final TurnMessageHandlerFactory messageHandlerFactory,
             final TurnReaderWriterFactory turnSocketFactory,
             final FuncWithReturn<Integer> portProvider)
        {
        m_turnMessageFactory = messageFactory;
        m_turnServerConnector = serverConnector;
        m_turnMessageHandlerFactory = messageHandlerFactory;
        m_turnSocketFactory = turnSocketFactory;
        m_portProvider = portProvider;
        }

    /**
     * Sets the handler for processing newly created sockets.
     *
     * @param socketHandler
     *      The handler for processing newly created sockets.
     */
    public void setSocketHandler
            (final SocketHandler socketHandler)
        {
        m_socketHandler = socketHandler;
        }

    /**
     * The connector listener to be notified of connection events.
     */
    private class MyConnectorListener implements ReadWriteConnectorListener
        {
        /**
         * The server listener to be notified of connection events.
         */
        private final TurnServerListener m_serverListener;

        /**
         * Constructs a new connector listener.
         *
         * @param serverListener
         *      The server listener to be notified of connection events.
         */
        public MyConnectorListener
                (final TurnServerListener serverListener)
            {
            m_serverListener = serverListener;
            }

        /**
         * {@inheritDoc}
         */
        public void onConnect
                (final ReaderWriter readerWriter)
            {
            final TurnReaderWriterTracker tracker =
                    new TurnReaderWriterTrackerImpl ();

            final DataIndicationListener dataListener =
                    new DataIndicationListenerImpl (tracker,
                                                    m_turnSocketFactory,
                                                    m_socketHandler,
                                                    m_portProvider);

            final TurnMessageVisitor visitor =
                    new TurnClientMessageVisitor (m_serverListener,
                                                  dataListener, readerWriter,
                                                  m_turnMessageFactory,
                                                  tracker);

            final ProtocolHandler protocolHandler =
                    new TurnProtocolHandler (m_turnMessageHandlerFactory,
                                             visitor);

            readerWriter.setProtocolHandler (protocolHandler);

            final TurnMessage msg =
                    m_turnMessageFactory.createAllocateRequest ();

            LOG.trace ("Sending allocate request to write handler...");
            readerWriter.writeLater (msg.toByteBuffers ());

            // We do not notify the server listener of connection until we
            // successfully visit a TURN message.
            }

        /**
         * {@inheritDoc}
         */
        public void onConnectFailed
                (final InetSocketAddress address)
            {
            LOG.trace ("Connect failed for: " + address.getAddress ());

            m_serverListener.connectionFailed ();
            }
        }

    /**
     * {@inheritDoc}
     */
    public void allocate
            (final InetSocketAddress address,
             final TurnServerListener listener)
        {
        LOG.trace ("Attempting to allocate: " + address);

        final Collection servers = new LinkedList ();

        servers.add (address);

        m_turnServerConnector.connect (servers,
                                       new MyConnectorListener (listener));
        }
    }

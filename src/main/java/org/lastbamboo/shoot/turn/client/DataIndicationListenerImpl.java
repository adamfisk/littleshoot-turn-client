package org.lastbamboo.shoot.turn.client;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.shoot.protocol.ReaderWriter;
import org.lastbamboo.shoot.turn.message.DataIndication;
import org.lastbamboo.util.FuncWithReturn;
import org.lastbamboo.util.SocketHandler;

/**
 * Class that listens for TURN "Data Indication" messages and delivers them
 * to the appropriate client handler classes.  If there is not an existing
 * socket for processing data from the REMOTE-ADDRESS attribute in the
 * message, then a new socket is created.
 */
public final class DataIndicationListenerImpl implements DataIndicationListener
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG =
        LogFactory.getLog(DataIndicationListenerImpl.class);

    private final TurnReaderWriterTracker m_turnReaderWriterTracker;

    private final TurnReaderWriterFactory m_turnReaderWriterFactory;

    /**
     * The class that handles newly created sockets for data from new remote
     * hosts.  This is common, for example, when we're responding to new HTTP
     * requests instead of pipelining.
     */
    private final SocketHandler m_socketHandler;
    
    /**
     * The function that returns the port to use for sockets when creating reader/writers.
     */
    private final FuncWithReturn<Integer> m_portProvider;


    /**
     * Creates a new listener for responding to TURN data indication messages.
     *
     * @param turnSocketTracker Class that keeps track of sockets allocated
     * for remote hosts.
     * @param turnSocketFactory The factory for creating sockets for remote
     * hosts.
     */
    public DataIndicationListenerImpl(
        final TurnReaderWriterTracker turnSocketTracker,
        final TurnReaderWriterFactory turnSocketFactory,
        final SocketHandler socketHandler,
        final FuncWithReturn<Integer> portProvider)
        {
        this.m_turnReaderWriterTracker = turnSocketTracker;
        this.m_turnReaderWriterFactory = turnSocketFactory;
        this.m_socketHandler = socketHandler;
        this.m_portProvider = portProvider;
        }

    public void onDataIndication(final TurnServerWriter turnWriter,
        final DataIndication dataIndication)
        {
        try
            {
            final ReaderWriter readerWriter =
                createReaderWriter(turnWriter, dataIndication);
            LOG.trace("Created ReaderWriter...");

            LOG.trace("Writing "+dataIndication.getData().remaining()+
                " bytes...");
            readerWriter.writeLater(dataIndication.getData());
            }
        catch (final IOException e)
            {
            // We could not connect to the local server.  Not sure what to
            // do in this case.
            LOG.error("Could not connect to local server", e);
            }
        }

    /**
     * Creates a <code>ReaderWriter</code> instance either from an existing
     * <code>ReaderWriter</code> already created for the remote host, or a
     * new one if we haven't yet allocated a <code>ReaderWriter</code> for
     * that host.
     *
     * @param turnWriter The class for writing messages to the TURN server.
     * @param dataIndication The message containing data from a remote host.
     * @return The <code>ReaderWriter</code> for the remote host.
     * @throws IOException If we could not create a reader/writer to the local
     * server.
     */
    private ReaderWriter createReaderWriter(final TurnServerWriter turnWriter,
        final DataIndication dataIndication) throws IOException
        {
        final InetSocketAddress remoteAddress = dataIndication.getRemoteHost();
        final ReaderWriter readerWriter;

        // Use an existing reader/writer if it's there.  This is not normally
        // invoked over the course of typical operation because remote hosts
        // will typically create new connections each time they want to send
        // a message, particularly for HTTP.  If remote hosts want to send more
        // data along the same connection for any reason, however, this allows
        // it.
        if (this.m_turnReaderWriterTracker.hasReaderWriter(remoteAddress))
            {
            LOG.trace("Using existing reader/writer...");
            readerWriter =
                this.m_turnReaderWriterTracker.getReaderWriter(remoteAddress);
            }

        // If there's not an existing connection for the remote host, we just
        // create a new one.
        else
            {
            LOG.trace("Creating new reader/writer...");

            // This will send the new socket to the HTTP server.
            readerWriter =
                this.m_turnReaderWriterFactory.createReaderWriter(remoteAddress,
                    turnWriter, this.m_socketHandler, m_portProvider.run());
            this.m_turnReaderWriterTracker.addReaderWriter(remoteAddress,
                readerWriter);
            }
        return readerWriter;
        }
    }

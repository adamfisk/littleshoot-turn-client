package org.lastbamboo.common.turn.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.nio.AcceptorListener;
import org.lastbamboo.common.nio.NioReaderWriter;
import org.lastbamboo.common.nio.SelectorManager;
import org.lastbamboo.common.protocol.ProtocolHandler;
import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.common.util.NetworkUtils;
import org.lastbamboo.common.util.SocketHandler;

/**
 * Factory for creating TURN sockets.  This creates real sockets running
 * through a local web server to remove some of the complexity from
 * tunneling data through TURN "Send Request" messages and TURN
 * "Data Indication" messages.
 */
public final class TurnReaderWriterFactoryImpl implements
    TurnReaderWriterFactory, AcceptorListener
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG =
        LogFactory.getLog(TurnReaderWriterFactoryImpl.class);
    private final SelectorManager m_readSelector;
    private volatile InetSocketAddress m_remoteHost;
    private volatile TurnServerWriter m_turnServerWriter;
    private volatile ReaderWriter m_readerWriter;

    /**
     * Creates a new class for creating <code>ReaderWriter</code>s
     * @param readSelector The selector to register with for read and write
     * events.
     */
    public TurnReaderWriterFactoryImpl(final SelectorManager readSelector)
        {
        this.m_readSelector = readSelector;
        }

    public ReaderWriter createReaderWriter(
        final InetSocketAddress destinationAddress,
        final TurnServerWriter turnServer,
        final SocketHandler socketHandler,
        final int localPort)
        throws IOException
        {
        this.m_remoteHost = destinationAddress;
        this.m_turnServerWriter = turnServer;
        this.m_readerWriter = null;

        connectToLocalServer(socketHandler, localPort);
        LOG.trace("Created client socket to server...");
        waitForSocket();
        LOG.trace("Finished waiting for socket...");
        return this.m_readerWriter;
        }

    /**
     * Waits for the local server to accept the new client socket.
     * @throws IOException If we could not successully connect to the local
     * server.
     */
    private void waitForSocket() throws IOException
        {
        synchronized (this)
            {
            if (this.m_readerWriter == null)
                {
                LOG.trace("Reader/writer is "+this.m_readerWriter+
                    "...waiting..."+this);
                try
                    {
                    wait(10000);
                    LOG.trace("Finished waiting...");
                    if (this.m_readerWriter == null)
                        {
                        LOG.error("Could not create reader/writer");
                        throw new IOException(
                            "Could not connect to local server!!");
                        }
                    }
                catch (final InterruptedException e)
                    {
                    LOG.error("Unexpected interrupt in socket creation", e);
                    }
                }
            else
                {
                // We've already created the reader/writer.
                return;
                }
            }
        }

    /**
     * Connects to the local server.  Note that the local server is registered
     * for accept events on its own selector, so it will accept the socket on
     * a different thread, allowing the wait and notify to work here.
     * @param socketHandler The handler that will process the newly created
     * socket.
     * @throws IOException If we could not connect to the local server
     * successfully.
     */
    private void connectToLocalServer(final SocketHandler socketHandler,
                                      final int localPort)
        throws IOException
        {
        LOG.trace("Connecting to local server with instance: "+this);
        final Socket clientSocket = new Socket();
        final InetSocketAddress localServer =
            new InetSocketAddress(NetworkUtils.getLocalHost(), localPort);

        clientSocket.connect(localServer, 10000);

        if (!clientSocket.isConnected())
            {
            LOG.error("Could not connect to server...");
            }

        LOG.trace("Sending socket to handler...");
        // This does not block.  Instead, it gets handed off to a thread
        // pool in the typical case.
        socketHandler.handleSocket(clientSocket);
        }

    public void onAccept(final SocketChannel sc)
        {
        LOG.trace("Accepted socket!!");
        // TODO: Test to make sure we don't accept remote sockets.
        try
            {
            // Create a separate protocol handler for each connection because
            // each one has a unique destination address for send requests.
            final ProtocolHandler protocolHandler =
                new TurnProxyProtocolHandler(this.m_remoteHost,
                    this.m_turnServerWriter, sc, this.m_readSelector);

            LOG.trace("About to create reader/writer...");
            this.m_readerWriter =
                new NioReaderWriter(sc, this.m_readSelector, protocolHandler);
            }
        catch (final SocketException e)
            {
            // The reader/writer will still be null here, and the appropriate
            // exceptions will get thrown.
            LOG.error("Unexpected exception on client proxy socket", e);
            }
        catch (final IOException e)
            {
            // The reader/writer will still be null here, and the appropriate
            // exceptions will get thrown.
            LOG.error("Unexpected exception on client proxy socket", e);
            }

        LOG.trace("Notifying waiters...reader/writer is: "+
            this.m_readerWriter+"  "+this);
        synchronized (this)
            {
            notifyAll();
            }
        }

    }

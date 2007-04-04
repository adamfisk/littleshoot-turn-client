package org.lastbamboo.common.turn.client;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.common.util.SocketHandler;

/**
 * Interface for class that creates TURN sockets through the locally-running
 * TURN client proxy server.
 */
public interface TurnReaderWriterFactory
    {

    /**
     * Creates a <code>ReaderWriter</code> for sending data from the specified
     * remote host.
     *
     * @param destinationAddress The address of the remote host to exchange
     * data with.
     * @param handler The handler for any newly created sockets.
     * @param turnServerWriter The TURN server this socket is communicating
     * with.
     * @param localPort The port on which to create sockets.
     * @return The <code>ReaderWriter</code> for sending data from the
     * remote host.
     * @throws IOException If there's an error connecting to the locally-running
     * server.
     */
    ReaderWriter createReaderWriter(
        final InetSocketAddress destinationAddress,
        final TurnServerWriter turnServerWriter, final SocketHandler handler,
        int localPort)
        throws IOException;
    }

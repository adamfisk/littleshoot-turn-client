package org.lastbamboo.shoot.turn.client;

import java.net.InetSocketAddress;

import org.lastbamboo.common.protocol.ReaderWriter;

/**
 * Interface for classes that keep track of TURN "sockets" to remote hosts.
 */
public interface TurnReaderWriterTracker
    {

    /**
     * Checks whether or not there is an existing socket for the specified
     * remote address.
     * @param remoteAddress The remote address to check for an existing socket.
     * @return <code>true</code> if there's already a socket for the specified
     * remote address, otherwise <code>false</code>.
     */
    boolean hasReaderWriter(final InetSocketAddress remoteAddress);

    /**
     * Adds the specified <code>SocketChannel</code> for the specified remote 
     * host.
     * 
     * @param remoteAddress The address and port of the remote host to create
     * a socket for.
     * @param readerWriter The channel for that host.
     */
    void addReaderWriter(final InetSocketAddress remoteAddress, 
        final ReaderWriter readerWriter);

    /**
     * Accessor for the <code>SocketChannel</code> for the remote address.
     * @param remoteAddress The remote address to access a socket for.
     * @return The <code>SocketChannel</code> for the remote address.
     */
    ReaderWriter getReaderWriter(final InetSocketAddress remoteAddress);

    /**
     * Removes the channel associated with the specified host.
     * @param remoteAddress The ip and port of the remote host to close the
     * channel with.
     */
    void removeReaderWriter(final InetSocketAddress remoteAddress);

    }

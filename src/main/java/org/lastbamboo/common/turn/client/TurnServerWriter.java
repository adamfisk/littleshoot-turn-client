package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * Interface for a connection to a TURN server.
 */
public interface TurnServerWriter 
    {
    
    /**
     * Accessor for the mapped address for this client on the TURN server.
     * @return The mapped address on the server.
     */
    InetSocketAddress getMappedAddress();
    
    /**
     * Writes the specied data in a TURN "Send Request" for delivery through
     * the TURN server to the specified host.  This call blocks until the
     * server has sent a response or the request has timed out.  If the
     * server sends a "Send Response", this return <code>true</code> to 
     * indicate the request was sent successfully.  If the server replies with
     * a "Send Error Response", however, this returns <code>false</code> to
     * indicate the request did not complete successfully.
     * 
     * @param destinationAddress The address of the remote host to send the
     * data to.
     * @param data The data to encapsulate in the Send Request for delivery to
     * the remote host.
     * @return <code>true</code> if the TURN server returns a "Send Response"
     * message indicating successful processing, or <code>false</code> if
     * the server sent a "Send Error Response" indicating the request did
     * not go through successfully, possibly indicating the remote host has
     * closed the connection.
     */
    boolean writeSendRequest(final InetSocketAddress destinationAddress, 
        final ByteBuffer data);

    }

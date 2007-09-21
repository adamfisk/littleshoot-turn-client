package org.lastbamboo.common.turn.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.lastbamboo.common.stun.client.StunClient;
import org.lastbamboo.common.util.ConnectionMaintainerListener;

/**
 * Interface for the local TURN client. 
 */
public interface TurnClient extends StunClient
    {

    /**
     * Tells the client to connect to its TURN server.
     * 
     * @param stunServerAddress The address of the STUN server to connect to. 
     * @param listener The listener for connection status to maintain 
     * connections. 
     */
    void connect(ConnectionMaintainerListener<InetSocketAddress> listener, 
        InetSocketAddress stunServerAddress);
    
    /**
     * Tells the client to connect to its TURN server.
     * 
     * @param stunServerAddress The address of the STUN server to connect to. 
     * @param listener The listener for connection status to maintain 
     * connections. 
     * @param localAddress The local address to bind to.  Binds to an ephemeral
     * port if the local address is <code>null</code>.
     */
    void connect(ConnectionMaintainerListener<InetSocketAddress> listener, 
        InetSocketAddress stunServerAddress, InetSocketAddress localAddress);

    /**
     * Accesses the allocated address for this TURN client on the TURN server.
     * 
     * @return The address the TURN server allocated on this client's behalf.
     */
    InetSocketAddress getRelayAddress();

    /**
     * Tells the client to send a Connect Request for the specified remote
     * address, telling the TURN server to attempt to connect to the specified
     * address and to allow incoming connections from that address.
     * 
     * @param remoteAddress The address of the remote host.
     */
    void sendConnectRequest(InetSocketAddress remoteAddress);

    /**
     * Closes this client's connection to the TURN server.
     */
    void close();

    /**
     * Accesses the MAPPED ADDRESS attribute returned from the TURN server.
     * This is the server reflexive, or public address.
     * 
     * @return The address and port returned in the MAPPED ADDRESS attribute.
     */
    InetSocketAddress getMappedAddress();

    /**
     * Gets the address of the STUN server this TURN client is using.
     * 
     * @return The address of the STUN server.
     */
    InetAddress getStunServerAddress();

    boolean isConnected();

    }

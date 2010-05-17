package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;

import org.littleshoot.mina.common.IoSession;

/**
 * Interface for classes that listen for TURN client events. 
 */
public interface TurnClientListener
    {

    IoSession onRemoteAddressOpened(InetSocketAddress remoteAddress, 
        IoSession session);

    void onRemoteAddressClosed(InetSocketAddress remoteAddress);

    void close();

    void onData(InetSocketAddress remoteAddress, IoSession session, byte[] data);

    }

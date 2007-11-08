package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;

import org.lastbamboo.common.stun.stack.message.StunMessage;

/**
 * Helper class for TURN that maps STUN transaction IDs to remote addresses. 
 */
public interface TurnStunMessageMapper
    {

    void mapMessage(StunMessage message, InetSocketAddress remoteAddress);

    InetSocketAddress get(StunMessage msg);

    }

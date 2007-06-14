package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.lastbamboo.common.util.ConnectionEstablisher;
import org.lastbamboo.common.util.ConnectionMaintainerListener;

/**
 * The connection establisher used to establish connections with TURN servers.
 */
public final class TurnServerConnectionEstablisher
        implements ConnectionEstablisher<InetSocketAddress, InetSocketAddress>
    {
    /**
     * The log for this class.
     */
    private static final Log LOG =
        LogFactory.getLog (TurnServerConnectionEstablisher.class);

    /**
     * {@inheritDoc}
     */
    public void establish
            (final InetSocketAddress serverId,
             final ConnectionMaintainerListener<InetSocketAddress> listener)
        {
        LOG.debug ("Establishing connection to: " + serverId);

        final TurnClient client = new TurnClientImpl();
        client.connect(listener, serverId);
        }
    }
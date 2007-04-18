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
        implements ConnectionEstablisher<InetSocketAddress,TurnServerWriter>
    {
    /**
     * The log for this class.
     */
    private static final Log LOG =
            LogFactory.getLog (TurnServerConnectionEstablisher.class);

    /**
     * The allocation manager used to allocate TURN server addresses.
     */
    private final TurnAllocationManager m_allocationManager;

    /**
     * Constructs a new TURN server connection establisher.
     *
     * @param allocationManager
     *      The allocation manager used to allocate TURN server addresses.
     */
    public TurnServerConnectionEstablisher
            (final TurnAllocationManager allocationManager)
        {
        m_allocationManager = allocationManager;
        }

    /**
     * {@inheritDoc}
     */
    public void establish
            (final InetSocketAddress serverId,
             final ConnectionMaintainerListener<TurnServerWriter> listener)
        {
        LOG.debug ("establish: " + serverId);

        final TurnServerListener turnServerListener = new TurnServerListener ()
            {
            public void connected
                    (final TurnServerWriter writer)
                {
                listener.connected (writer);
                }

            public void connectionFailed
                    ()
                {
                listener.connectionFailed ();
                }

            public void disconnected
                    ()
                {
                listener.disconnected ();
                }
            };

        m_allocationManager.allocate (serverId, turnServerListener);
        }
    }

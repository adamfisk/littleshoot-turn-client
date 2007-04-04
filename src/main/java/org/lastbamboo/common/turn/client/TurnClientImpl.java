package org.lastbamboo.common.turn.client;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.util.ConnectionMaintainer;

/**
 * Implementation of the TURN client facade, providing an API for external
 * code to use TURN client services.  This primarily dispatches tasks to
 * underlying collaborating classes.
 */
public final class TurnClientImpl implements TurnClient
    {
    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog (TurnClientImpl.class);

    /**
     * The connection maintainer used to maintain connections to TURN servers.
     */
    private final ConnectionMaintainer m_connectionMaintainer;

    /**
     * Creates a new TURN client.
     *
     * @param connectionMaintainer
     *      The manager for managing binding allocations on TURN servers.
     */
    public TurnClientImpl(final ConnectionMaintainer connectionMaintainer)
        {
        m_connectionMaintainer = connectionMaintainer;
        }

    /**
     * {@inheritDoc}
     */
    public void start()
        {
        LOG.trace ("Starting TURN client");

        // We start the connection maintainer.
        m_connectionMaintainer.start ();
        }

    /**
     * {@inheritDoc}
     */
    public Collection getTurnServers()
        {
        LOG.debug ("Connected servers size: " +
                        m_connectionMaintainer.getConnectedServers ().size ());

        // This can return null.
        return (m_connectionMaintainer.getConnectedServers ());
        }
    }

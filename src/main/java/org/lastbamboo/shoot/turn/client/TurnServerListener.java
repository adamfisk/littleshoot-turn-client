package org.lastbamboo.shoot.turn.client;

/**
 * The listener that is notified of TURN server connection events.
 */
public interface TurnServerListener
    {
    /**
     * Called on a successful connection.
     *
     * @param writer
     *      The writer representing the TURN server connection.
     */
    void connected
            (final TurnServerWriter writer);

    /**
     * Called on a failed attempt at connection.
     */
    void connectionFailed
            ();

    /**
     * Called when we are disconnected.
     */
    void disconnected
            ();
    }

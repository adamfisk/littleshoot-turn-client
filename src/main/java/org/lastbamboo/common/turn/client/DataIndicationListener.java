package org.lastbamboo.common.turn.client;

import org.lastbamboo.common.turn.message.DataIndication;

/**
 * Listener for TURN "Data Indication" messages.
 */
public interface DataIndicationListener
    {

    /**
     * Called when a TURN "Data Indication" message is received.
     * 
     * @param turnWriter The class for writing messages to the TURN server.
     * @param dataIndication The message containing data from a remote host.
     */
    void onDataIndication(final TurnServerWriter turnWriter, 
        final DataIndication dataIndication);
    }

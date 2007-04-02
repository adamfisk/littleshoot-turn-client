package org.lastbamboo.shoot.turn.client;

import org.lastbamboo.shoot.turn.message.SendErrorResponse;
import org.lastbamboo.shoot.turn.message.SendResponse;

/**
 * Interface for class that wish to process TURN Send Response and 
 * Send Error Response messages.
 */
public interface SendResponseHandler
    {

    /**
     * Handles the "Send Response" message received from the TURN server.
     * @param response The response to handle.
     */
    void handleSendResponse(final SendResponse response);
    
    /**
     * Handles the "Send Error Response" message received from the TURN server.
     * @param response The response to handle.
     */
    void handleSendErrorResponse(final SendErrorResponse response);
    }

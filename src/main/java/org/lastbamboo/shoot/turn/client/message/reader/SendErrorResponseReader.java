package org.lastbamboo.shoot.turn.client.message.reader;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.lastbamboo.shoot.turn.message.TurnMessageFactory;
import org.lastbamboo.shoot.turn.message.attribute.ErrorCodeAttribute;
import org.lastbamboo.shoot.turn.message.attribute.StunAttributeTypes;
import org.lastbamboo.shoot.turn.message.attribute.reader.TurnAttributesReader;
import org.lastbamboo.shoot.turn.message.handler.AbstractTurnMessageHandler;

/**
 * Reader for TURN "Send Error Response" messages.
 */
public final class SendErrorResponseReader extends AbstractTurnMessageHandler
    {

    /**
     * Creates a new reader for processing TURN "Send Error Response" messages.
     * 
     * @param turnServerSocketAddress The address and port of the TURN server.
     * @param factory The factory for creating TURN messages from the read
     * data.
     * @param attributesReader The reader for reading TURN attributes.
     */
    protected SendErrorResponseReader(
        final InetSocketAddress turnServerSocketAddress, 
        final TurnMessageFactory factory, 
        final TurnAttributesReader attributesReader)
        {
        super(turnServerSocketAddress, factory, attributesReader);
        }

    public void handleMessage() throws IOException
        {
        final ErrorCodeAttribute errorCodeAttribute =
            (ErrorCodeAttribute) this.m_attributes.get(
                new Integer(StunAttributeTypes.ERROR_CODE));
        
        // The Send Error Response message requires an ERROR CODE attribute.
        if (errorCodeAttribute == null)
            {
            throw new IOException("Could not read response--no ERROR CODE");
            }
        this.m_turnMessage = 
            this.m_turnMessageFactory.createSendErrorResponse(
                this.m_transctionId, errorCodeAttribute.getErrorCode());
        }

    }

package org.lastbamboo.shoot.turn.client.message.reader;

import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.shoot.turn.message.TurnMessageFactory;
import org.lastbamboo.shoot.turn.message.attribute.reader.TurnAttributesReader;
import org.lastbamboo.shoot.turn.message.handler.AbstractTurnMessageHandler;

/**
 * Handler for a response to send request.
 */
public final class SendResponseReader extends AbstractTurnMessageHandler
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG = LogFactory.getLog(SendResponseReader.class);
    
    /**
     * Creates a new handler for a send response message.
     * 
     * @param client The address of the remote host sending the message.
     * @param factory The factory for creating TURN messages.
     * @param attributesReader The class for reading TURN message attributes.
     */
    public SendResponseReader(final InetSocketAddress client, 
        final TurnMessageFactory factory, 
        final TurnAttributesReader attributesReader)
        {
        super(client, factory, attributesReader);
        }

    public void handleMessage()
        {
        LOG.trace("Handling send response!!!");
        this.m_turnMessage = 
            this.m_turnMessageFactory.createSendResponse(this.m_transctionId);
        }
    }

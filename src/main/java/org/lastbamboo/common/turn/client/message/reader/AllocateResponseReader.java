package org.lastbamboo.common.turn.client.message.reader;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.turn.message.TurnMessageFactory;
import org.lastbamboo.common.turn.message.attribute.InetSocketAddressTurnAttribute;
import org.lastbamboo.common.turn.message.attribute.StunAttributeTypes;
import org.lastbamboo.common.turn.message.attribute.reader.TurnAttributesReader;
import org.lastbamboo.common.turn.message.handler.AbstractTurnMessageHandler;

/**
 * Handler for processing TURN allocate response messages.  These are the 
 * messages that tell us what IP and port to report to other clients for how
 * to reach us.
 */
public final class AllocateResponseReader extends AbstractTurnMessageHandler
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG = 
        LogFactory.getLog(AllocateResponseReader.class);
    
    /**
     * Creates a new response handler.
     * @param client The address of the remote host sending the message.
     * @param factory The factory for creating TURN messages.
     * @param readerWriter The handler for writing message back to the client.
     * @param reader The class for reading TURN message attributes.
     */
    public AllocateResponseReader(final InetSocketAddress client, 
        final TurnMessageFactory factory, final TurnAttributesReader reader)
        {
        super(client, factory, reader);
        }

    public void handleMessage() throws IOException
        {
        LOG.trace("Handling message...");
        
        final InetSocketAddressTurnAttribute mappedAddressAttribute =
            (InetSocketAddressTurnAttribute) this.m_attributes.get(
                new Integer(StunAttributeTypes.MAPPED_ADDRESS));

        if (mappedAddressAttribute == null)
            {
            throw new IOException("Could not read response--no MAPPED-ADDRESS");
            }
        
        final InetSocketAddress mappedAddress = 
            mappedAddressAttribute.getInetSocketAddress();
        
        this.m_turnMessage = this.m_turnMessageFactory.createAllocateResponse(
            this.m_transctionId, mappedAddress);
        }

    }

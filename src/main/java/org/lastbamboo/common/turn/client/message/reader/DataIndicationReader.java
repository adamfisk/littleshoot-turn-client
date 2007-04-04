package org.lastbamboo.common.turn.client.message.reader;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.shoot.turn.message.TurnMessageFactory;
import org.lastbamboo.shoot.turn.message.attribute.DataAttribute;
import org.lastbamboo.shoot.turn.message.attribute.InetSocketAddressTurnAttribute;
import org.lastbamboo.shoot.turn.message.attribute.TurnAttributeTypes;
import org.lastbamboo.shoot.turn.message.attribute.reader.TurnAttributesReader;
import org.lastbamboo.shoot.turn.message.handler.AbstractTurnMessageHandler;

/**
 * Creates a new class for handing "Data Indication" messages from the 
 * TURN server.
 */
public final class DataIndicationReader extends AbstractTurnMessageHandler
    {
    
    /**
     * Logger for this class.
     */
    private static final Log LOG = 
        LogFactory.getLog(DataIndicationReader.class);

    /**
     * Creates a new response handler.
     * @param client The address of the remote host sending the message.
     * @param factory The factory for creating TURN messages.
     * @param attributesReader The class for reading TURN message attributes.
     */
    public DataIndicationReader(final InetSocketAddress client, 
        final TurnMessageFactory factory, 
        final TurnAttributesReader attributesReader)
        {
        super(client, factory, attributesReader);
        }

    public void handleMessage() throws IOException
        {
        LOG.trace("HANDLING DATA INDICATION MESSAGE!!");      
        final DataAttribute data = 
            (DataAttribute) this.m_attributes.get(
                new Integer(TurnAttributeTypes.DATA));
        
        if (data == null)
            {
            throw new IOException("No DATA attribute found.");
            }
        final InetSocketAddressTurnAttribute remoteAddress = 
            (InetSocketAddressTurnAttribute) this.m_attributes.get(
                new Integer(TurnAttributeTypes.REMOTE_ADDRESS));
        
        if (remoteAddress == null)
            {
            throw new IOException("No REMOTE-ADDRESS attribute found...");
            }
        this.m_turnMessage = 
            this.m_turnMessageFactory.createDataIndication(data, remoteAddress);
        }
    }

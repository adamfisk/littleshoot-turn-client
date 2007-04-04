package org.lastbamboo.common.turn.client.message.reader;

import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.turn.message.TurnMessageFactory;
import org.lastbamboo.common.turn.message.TurnMessageTypes;
import org.lastbamboo.common.turn.message.attribute.reader.TurnAttributesReader;
import org.lastbamboo.common.turn.message.handler.TurnMessageHandler;
import org.lastbamboo.common.turn.message.handler.TurnMessageHandlerFactory;
import org.lastbamboo.common.turn.message.handler.UnknownMessageTypeHandler;

/**
 * Factory for creating message handlers for TURN clients.  This only processes
 * messages TURN clients would expect from the server.
 */
public final class ClientTurnMessageReaderFactory implements
    TurnMessageHandlerFactory
    {
    
    /**
     * Logger for this class.
     */
    private static final Log LOG = 
        LogFactory.getLog(ClientTurnMessageReaderFactory.class);
    
    /**
     * Factory for creating TURN messages.
     */
    private TurnMessageFactory m_turnMessageFactory;
    
    /**
     * Helper class for reading TURN attributes.
     */
    private TurnAttributesReader m_attributesReader;
    
    /**
     * Sets the factory to use for creating TURN messages.
     * @param factory The factory to use for creating TURN messages.
     */
    public void setTurnMessageFactory(final TurnMessageFactory factory)
        {
        this.m_turnMessageFactory = factory;
        }
    
    /**
     * Sets the class for reading TURN message attributes.
     * 
     * @param reader The class for reading TURN message attributes.
     */
    public void setAttributesReader(final TurnAttributesReader reader)
        {
        this.m_attributesReader = reader;
        }

    public TurnMessageHandler createTurnMessageHandler(final int type,
        final InetSocketAddress turnServerSocketAddress)
        {
        switch (type)
            {
            case TurnMessageTypes.ALLOCATE_RESPONSE:
                LOG.trace("Received Allocate Response!!!");
                return new AllocateResponseReader(turnServerSocketAddress, 
                    this.m_turnMessageFactory, this.m_attributesReader);
            
            case TurnMessageTypes.SEND_RESPONSE:
                LOG.trace("Received Send Response!!!!");
                return new SendResponseReader(turnServerSocketAddress, 
                    this.m_turnMessageFactory, this.m_attributesReader);
            
            case TurnMessageTypes.SEND_ERROR_RESPONSE:
                LOG.trace("Received Send Error Response!!");
                return new SendErrorResponseReader(turnServerSocketAddress,
                    this.m_turnMessageFactory, this.m_attributesReader);
                
            case TurnMessageTypes.DATA_INDICATION:
                LOG.trace("Received Data Indication!!!");
                return new DataIndicationReader(turnServerSocketAddress, 
                    this.m_turnMessageFactory, this.m_attributesReader);
            default:
                LOG.warn("Received unknown message type: "+type);
                return new UnknownMessageTypeHandler(type, turnServerSocketAddress, 
                    this.m_turnMessageFactory, this.m_attributesReader);
            }
        }
    }

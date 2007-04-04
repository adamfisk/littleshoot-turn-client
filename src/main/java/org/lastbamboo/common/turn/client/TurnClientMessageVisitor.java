package org.lastbamboo.common.turn.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.common.turn.message.AllocateRequest;
import org.lastbamboo.common.turn.message.AllocateResponse;
import org.lastbamboo.common.turn.message.DataIndication;
import org.lastbamboo.common.turn.message.SendErrorResponse;
import org.lastbamboo.common.turn.message.SendRequest;
import org.lastbamboo.common.turn.message.SendResponse;
import org.lastbamboo.common.turn.message.TurnMessageFactory;
import org.lastbamboo.common.turn.message.TurnMessageVisitor;

/**
 * Visitor for processing TURN messages for a TURN client.  This primarily
 * delegates message processing to more specialized classes for handling each
 * type of message.
 */
public final class TurnClientMessageVisitor implements TurnMessageVisitor
    {
    /**
     * Logger for this class.
     */
    private static final Log LOG =
        LogFactory.getLog (TurnClientMessageVisitor.class);

    /**
     * The server listener to be notified of connection events.
     */
    private final TurnServerListener m_serverListener;

    /**
     * The listener to be notified of data indication events.
     */
    private final DataIndicationListener m_dataIndicationListener;

    /**
     * TODO: I do not know what this is - jjc.
     */
    private final ReaderWriter m_readerWriter;

    /**
     * The factory for constructing TURN messages.
     */
    private final TurnMessageFactory m_turnMessageFactory;

    /**
     * TODO: I do not know what this is - jjc.
     */
    private TurnServerWriter m_turnServerWriter;

    /**
     * TODO: I do not know what this is - jjc.
     */
    private SendResponseHandler m_sendResponseHandler;

    /**
     * TODO: I do not know what this is - jjc.
     */
    private final TurnReaderWriterTracker m_turnReaderWriterTracker;

    /**
     * Creates a new visitor for visiting TURN messages read on a TURN client.
     *
     * @param serverListener
     *      The server listener to be notified of connection events.
     * @param dataListener
     *      The class for processing "Data Indication" messages.
     * @param readerWriter
     *      The class for reading and writing data to and from the TURN server.
     * @param messageFactory
     *      The class for creating TURN messages.
     * @param readerWriterTracker
     *      TODO: I do not know what this is - jjc.
     */
    public TurnClientMessageVisitor
            (final TurnServerListener serverListener,
             final DataIndicationListener dataListener,
             final ReaderWriter readerWriter,
             final TurnMessageFactory messageFactory,
             final TurnReaderWriterTracker readerWriterTracker)
        {
        if (serverListener == null)
            {
            throw (new NullPointerException ("null server tracker"));
            }
        if (dataListener == null)
            {
            throw (new NullPointerException ("null data listener"));
            }
        if (readerWriter == null)
            {
            throw (new NullPointerException ("null reader/writer"));
            }
        if (messageFactory == null)
            {
            throw (new NullPointerException ("null message factory"));
            }
        if (readerWriterTracker == null)
            {
            throw (new NullPointerException ("null reader/writer tracker"));
            }

        m_serverListener = serverListener;
        m_dataIndicationListener = dataListener;
        m_readerWriter = readerWriter;
        m_turnMessageFactory = messageFactory;
        m_turnReaderWriterTracker = readerWriterTracker;
        }

    /**
     * {@inheritDoc}
     */
    public void visitAllocateResponse
            (final AllocateResponse response)
        {
        LOG.trace ("Received allocate response!!!");

        final TurnServerWriterImpl writer =
            new TurnServerWriterImpl (m_readerWriter, m_serverListener,
                m_turnMessageFactory, response.getMappedAddress (),
                m_turnReaderWriterTracker);

        m_sendResponseHandler = writer;
        m_turnServerWriter = writer;

        // When we get an allocate response, we consider our connection to the
        // TURN server complete.
        m_serverListener.connected (writer);
        }

    /**
     * {@inheritDoc}
     */
    public void visitSendResponse
            (final SendResponse response)
        {
        LOG.trace ("Received Send Response: " + response);
        m_sendResponseHandler.handleSendResponse (response);
        }

    /**
     * {@inheritDoc}
     */
    public void visitSendErrorResponse
            (final SendErrorResponse response)
        {
        LOG.trace ("Visiting Send Error Response: " + response);
        m_sendResponseHandler.handleSendErrorResponse (response);
        }

    /**
     * {@inheritDoc}
     */
    public void visitDataIndication
            (final DataIndication dataIndication)
        {
        LOG.trace ("Visiting Data Indication: " + dataIndication);
        m_dataIndicationListener.onDataIndication (m_turnServerWriter,
                                                   dataIndication);
        }

    /**
     * {@inheritDoc}
     */
    public void visitSendRequest
            (final SendRequest request)
        {
        // Ignored -- clients should never see this.
        LOG.error ("Client received Send Request....");
        }

    /**
     * {@inheritDoc}
     */
    public void visitAllocateRequest (final AllocateRequest request)
        {
        // Ignored -- clients should never see this.
        LOG.error ("Client received Allocate Request...");
        }
    }

package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.protocol.CloseListener;
import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.common.turn.message.SendErrorResponse;
import org.lastbamboo.common.turn.message.SendRequest;
import org.lastbamboo.common.turn.message.SendResponse;
import org.lastbamboo.common.turn.message.TurnMessageFactory;

/**
 * Manages a single TURN client connection to a TURN server.
 */
public final class TurnServerWriterImpl implements TurnServerWriter,
    CloseListener, SendResponseHandler
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG =
        LogFactory.getLog(TurnServerWriterImpl.class);

    private final ReaderWriter m_readerWriter;

    /**
     * The listener to be notified of connection events.
     */
    private final TurnServerListener m_serverListener;

    /**
     * The factory for creating TURN messages.
     */
    private final TurnMessageFactory m_turnMessageFactory;

    private final InetSocketAddress m_mappedAddress;

    private final TurnReaderWriterTracker m_readerWriterTracker;

    private volatile SendRequest m_currentRequest;

    private volatile boolean m_receivedResponse;

    /**
     * This is a lock object for waiting on an existing send request that
     * has not yet received a response.
     */
    private final Object RECEIVED_RESPONSE = new Object();

    /**
     * Flag for whether or not the server handled the last request successfully,
     * responding with a "Send Response" message and not a "Send Error
     * Response".
     */
    private volatile boolean m_requestSentSuccessfully;

    /**
     * Creates a new TURN server management class with the specified handler
     * for reading and writing data with the server.
     * @param readerWriter The handler for reading and writing data with the
     * server.
     * @param serverListener The class that listens for changes in the status
     * of our connection to the TURN server.
     * @param turnMessageFactory The factory for creating TURN messages.
     * @param mappedAddress The address allocated on the TURN server for this
     * host.
     * @param readerWriterTracker The class that keeps track of
     * <code>ReaderWriter</code>s to the local TURN proxy server.
     */
    public TurnServerWriterImpl(final ReaderWriter readerWriter,
        final TurnServerListener serverListener,
        final TurnMessageFactory turnMessageFactory,
        final InetSocketAddress mappedAddress,
        final TurnReaderWriterTracker readerWriterTracker)
        {
        this.m_readerWriter = readerWriter;
        this.m_serverListener = serverListener;
        this.m_turnMessageFactory = turnMessageFactory;
        this.m_mappedAddress = mappedAddress;
        this.m_readerWriter.addCloseListener(this);
        this.m_readerWriterTracker = readerWriterTracker;
        }

    public void onClose(final ReaderWriter readerWriter)
        {
        LOG.debug("Removing TURN server: "+readerWriter);
        m_serverListener.disconnected ();
        }

    public boolean writeSendRequest(final InetSocketAddress destinationAddress,
        final ByteBuffer data)
        {
        // We synchronize on 'this' to prevent multiple threads from sending
        // requests at the same time.  We prevent this since each request must
        // wait for a response.
        synchronized (this)
            {
            final SendRequest request =
                this.m_turnMessageFactory.createSendRequest(destinationAddress,
                    data);

            LOG.trace("Writing Send Request with data length: "+
                data.remaining()+" on writer: "+this);
            this.m_receivedResponse = false;
            this.m_currentRequest = request;
            this.m_readerWriter.writeLater(request.toByteBuffers());
            LOG.trace("Wrote Send Request...");

            // At this point, we wait for a response.
            synchronized (RECEIVED_RESPONSE)
                {
                LOG.trace("Waiting for send response...");
                // If we have not yet received a response, wait for a response.
                // The RECEIVED_RESPONSE object will be notified when a
                // response is received.
                if (!this.m_receivedResponse)
                    {
                    try
                        {
                        // We include a timeout in case no response is
                        // received.
                        RECEIVED_RESPONSE.wait(30 * 1000);
                        }
                    catch (final InterruptedException e)
                        {
                        LOG.error("Unexpected interrupt.", e);
                        }
                    }

                if (!this.m_receivedResponse)
                    {
                    // We get here if we waited for a notification on
                    // RECEIVED_RESPONSE but timed out before we received such
                    // notification.
                    LOG.error("No response to the send request.");
                    closeTurnClient();
                    }

                boolean toReturn = this.m_requestSentSuccessfully;
                this.m_receivedResponse = false;
                this.m_currentRequest = null;
                this.m_requestSentSuccessfully = false;
                LOG.trace("Done processing Send Request...");
                return toReturn;
                }
            }
        }

    /**
     * Closes the connection to the TURN server.  Note that the reader/writer
     * is also automatically removed from the TURN client tracker because the
     * tracker listens for close events.
     */
    private void closeTurnClient()
        {
        this.m_readerWriter.close();
        }

    public InetSocketAddress getMappedAddress()
        {
        return this.m_mappedAddress;
        }

    public void handleSendResponse(final SendResponse response)
        {
        synchronized (RECEIVED_RESPONSE)
            {
            LOG.trace("Handling Send Response...");
            if (!this.m_currentRequest.getTransactionId().equals(
                response.getTransactionId()))
                {
                LOG.error("Transaction IDs do not match!!");
                }
            this.m_receivedResponse = true;
            this.m_requestSentSuccessfully = true;
            RECEIVED_RESPONSE.notifyAll();
            }
        }

    public void handleSendErrorResponse(final SendErrorResponse response)
        {
        synchronized (RECEIVED_RESPONSE)
            {
            LOG.trace("Handling Send Error Response...");
            if (!this.m_currentRequest.getTransactionId().equals(
                response.getTransactionId()))
                {
                LOG.error("Transaction IDs do not match!!");
                }

            // We now close the local reader/writer.  This ultimately closes the
            // socket from the web server's perspective, allowing the web server
            // to stop servicing the request and to recycle any associated
            // resources.
            final InetSocketAddress destinationAddress =
                this.m_currentRequest.getDestinationAddress();
            final ReaderWriter localReaderWriter =
                this.m_readerWriterTracker.getReaderWriter(destinationAddress);

            // The send request might not have come from a local socket, but
            // could have just been a send request to allow permissions for
            // the remote host.
            if (localReaderWriter != null)
                {
                localReaderWriter.close();
                }
            this.m_readerWriterTracker.removeReaderWriter(destinationAddress);

            this.m_receivedResponse = true;
            this.m_requestSentSuccessfully = false;
            RECEIVED_RESPONSE.notifyAll();
            }
        }

    }

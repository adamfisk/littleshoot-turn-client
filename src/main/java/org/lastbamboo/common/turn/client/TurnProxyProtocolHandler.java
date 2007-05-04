package org.lastbamboo.common.turn.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.nio.SelectorManager;
import org.lastbamboo.common.protocol.ProtocolHandler;
import org.lastbamboo.common.util.ByteBufferUtils;

/**
 * Protocol handler that simply takes data read in from local sockets and 
 * sends it along to the TURN server for this handler wrapped in a TURN
 * "Send Request".  We need a separate handler for each TURN client socket
 * because each one has a unique destination address it's exchanging data
 * with.
 */
public final class TurnProxyProtocolHandler implements ProtocolHandler
    {

    /**
     * Logger for this class.
     */
    private static final Log LOG = 
        LogFactory.getLog(TurnProxyProtocolHandler.class);
    
    /**
     * This is the limit on the length of the data to encapsulate in a Send
     * Request.  TURN messages cannot be larger than 0xffff, so this leaves 
     * room for other attributes in the message as well as for headers.
     */
    private static final int LENGTH_LIMIT = 0xffff - 1000;
    
    private final TurnServerWriter m_turnServerWriter;
    private final InetSocketAddress m_destinationAddress;

    private final SocketChannel m_socketChannel;

    private final SelectorManager m_selector;
    
    /**
     * Class for processing incoming data in order.
     */
    private final ExecutorService m_executorService = 
        Executors.newSingleThreadExecutor();

    /**
     * Creates a new handler for exchanging data with the specified remote host.
     * @param destinationAddress The remote host to exchange data with via the
     * TURN server.
     * @param turnServerWriter The class for writing data to the TURN server.
     * @param selector The selector that's sending events to this handler.
     * @param sc The channel for this handler is processing data for.
     */
    public TurnProxyProtocolHandler(final InetSocketAddress destinationAddress,
        final TurnServerWriter turnServerWriter, final SocketChannel sc, 
        final SelectorManager selector)
        {
        this.m_destinationAddress = destinationAddress;
        this.m_turnServerWriter = turnServerWriter;
        this.m_socketChannel = sc;
        this.m_selector = selector;
        }
    
    public void handleMessages(final ByteBuffer buffer, 
        final InetSocketAddress localHost)
        {        
        // Just wrap the data in a Send Request and send it along to the TURN
        // server.
        
        // TODO: How can we avoid making this copy while not writing the
        // entire TCP buffer??
        buffer.flip();
        LOG.trace("Reading "+buffer.remaining()+" bytes from local " +
            "client and wraping them in a Send Request...");
        if (!buffer.hasRemaining())
            {
            LOG.warn("No data in buffer!!");
            return;
            }
        
        removeReadInterest();
        
        sendBuffers(buffer);   
        }

    /**
     * Sends the specified buffer in a TURN "Send Request", either sending
     * the entire buffer in a single message or splitting it into multiple 
     * "Send Requests" if it's too big to fit in one TURN message.
     * 
     * @param buffer The main read buffer with data to send.
     */
    private void sendBuffers(final ByteBuffer buffer)
        {
        // If the buffer has a reasonable size, just send it along.
        if (buffer.remaining() < LENGTH_LIMIT)
            {
            sendBuffer(buffer);
            }        
        else
            {
            // Otherwise, break it up.
            sendSplitBuffers(buffer);
            }
        }

    /**
     * Sends a copy of the full buffer.
     * 
     * @param buffer The main read buffer with data to send.
     */
    private void sendBuffer(final ByteBuffer buffer)
        {
        final ByteBuffer data = ByteBuffer.allocate(buffer.limit());
        data.put(buffer);
        data.rewind();
        
        final Runnable sendRequestRunner = new Runnable()
            {
            public void run()
                {
                LOG.trace("Sending buffer...");
                if (sendData(data))
                    {
                    addReadInterest();
                    }
                }
            };
            
        this.m_executorService.submit(sendRequestRunner);
        }

    /**
     * Splits the main read buffer into smaller buffers that will fit in
     * TURN messages.
     * 
     * @param buffer The main read buffer to split.
     */
    private void sendSplitBuffers(final ByteBuffer buffer)
        {
        // Break up the data into smaller chunks.
        final Collection buffers = ByteBufferUtils.split(buffer, LENGTH_LIMIT);
        
        final Runnable sendRequestRunner = new Runnable()
            {
            public void run()
                {
                LOG.trace("Sending "+buffers.size()+" buffers...");
                for (final Iterator iter = buffers.iterator(); iter.hasNext();)
                    {
                    final ByteBuffer data = (ByteBuffer) iter.next();
                    if (!sendData(data))
                        {
                        return;
                        }
                    }
                
                addReadInterest();
                }
            };
        this.m_executorService.submit(sendRequestRunner);
        }
    
    /**
     * Sends the specified data to the TURN server.  If the send fails for
     * any reason, this will close the underlying channel.
     * 
     * @param data The data to send.
     * @return <code>true</code> if the data was successfully sent, otherwise
     * <code>false</code>
     */
    private boolean sendData(final ByteBuffer data)
        {
        LOG.trace("Sending buffer with " + data.remaining() + " bytes...");
        return m_turnServerWriter.writeSendRequest(m_destinationAddress, data);
        }
    
    /**
     * Remove interest in reading while we process the current data.
     */
    private void removeReadInterest()
        {
        try
            {
            this.m_selector.removeChannelInterestNow(this.m_socketChannel, 
                SelectionKey.OP_READ);
            }
        catch (final IOException e)
            {
            // This shouldn't happen, but we'll just keep going if it does.
            LOG.error("Could not remove read interest!!", e);
            }
        }
    
    /**
     * Returns interest in reading when we've completely sent all available
     * data.
     */
    private void addReadInterest()
        {
        // We're ready for reading again.
        this.m_selector.addChannelInterestLater(this.m_socketChannel, 
            SelectionKey.OP_READ);
        }
    }

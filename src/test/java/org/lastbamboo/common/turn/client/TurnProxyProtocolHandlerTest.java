package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.lastbamboo.common.nio.SelectorManager;
import org.lastbamboo.common.nio.SelectorManagerImpl;
import org.lastbamboo.common.protocol.ProtocolHandler;
import org.lastbamboo.common.turn.client.TurnProxyProtocolHandler;
import org.lastbamboo.common.turn.client.TurnServerWriter;
import org.lastbamboo.common.turn.client.stub.SocketChannelStub;

/**
 * Tests the handler for the locally-running server that handles traffic from
 * an individual socket channel and wraps it in send requests to forward to
 * the TURN server.
 */
public final class TurnProxyProtocolHandlerTest extends TestCase
    {
    
    /**
     * Tests sending messages that are too large for TURN messages to handle
     * due to the fact that the message length is specified in 2 bytes.
     * @throws Exception If any unexpected error occurs.
     */
    public void testTooLargeMessage() throws Exception
        {
        final InetSocketAddress destinationAddress =
            new InetSocketAddress("34.4.5.3", 4242); 
        
        // Allocate a bunch more bytes than can fit in a single message.
        final int minimumCalls = 10;
        final int bytes = 0xffff * minimumCalls + 1;
        
        final int chunkSize = 0xFFFF;
        final int expectedCalls = bytes/chunkSize + 1;
        final byte[] dataBytes = new byte[bytes];
        Arrays.fill(dataBytes, (byte) 7);
        
        final ByteBuffer data = ByteBuffer.allocate(dataBytes.length);
        
        for (int i = 0; i < dataBytes.length; i++)
            {
            data.put(dataBytes[i]);
            }
        
        final MockControl turnWriterControl = 
            MockControl.createControl(TurnServerWriter.class);
        final TurnServerWriter turnWriter = 
            (TurnServerWriter) turnWriterControl.getMock();
        turnWriter.writeSendRequest(destinationAddress, ByteBuffer.allocate(1));
        
        // Just make sure we get the right number of calls.
        turnWriterControl.setMatcher(MockControl.ALWAYS_MATCHER);
        turnWriterControl.setReturnValue(true, expectedCalls);
        turnWriterControl.replay();
        
        final SocketChannel socketChannel = new SocketChannelStub();
        final SelectorManager selector = new SelectorManagerImpl();
        selector.start();
        final ProtocolHandler handler = 
            new TurnProxyProtocolHandler(destinationAddress, turnWriter, 
                socketChannel, selector);
        
        handler.handleMessages(data, destinationAddress);
        Thread.sleep(2000);
        turnWriterControl.verify();
        }

    /**
     * Tests the method for handling data from local clients.
     * @throws Exception If any unexpected error occurs.
     */
    public void testHandleMessages() throws Exception
        {
        final InetSocketAddress destinationAddress =
            new InetSocketAddress("34.4.5.3", 4242);        
        final byte[] dataBytes = new byte[10];
        Arrays.fill(dataBytes, (byte) 7);
        
        final ByteBuffer data = ByteBuffer.allocate(dataBytes.length);
        
        for (int i = 0; i < dataBytes.length; i++)
            {
            data.put(dataBytes[i]);
            }
        
        final ByteBuffer expectedBuffer = data.duplicate();
        expectedBuffer.flip();
        final MockControl turnWriterControl = 
            MockControl.createControl(TurnServerWriter.class);
        final TurnServerWriter turnWriter = 
            (TurnServerWriter) turnWriterControl.getMock();
        turnWriter.writeSendRequest(destinationAddress, expectedBuffer);
        turnWriterControl.setReturnValue(true, 1);
        turnWriterControl.replay();
        final SocketChannel socketChannel = new SocketChannelStub();
        final SelectorManager selector = new SelectorManagerImpl();
        selector.start();
        final ProtocolHandler handler = 
            new TurnProxyProtocolHandler(destinationAddress, turnWriter, 
                socketChannel, selector);
        
        handler.handleMessages(data, destinationAddress);
        
        Thread.sleep(2000);
        turnWriterControl.verify();
        }
    }

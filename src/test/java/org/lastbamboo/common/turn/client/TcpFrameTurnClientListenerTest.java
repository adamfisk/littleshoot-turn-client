package org.lastbamboo.common.turn.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.math.RandomUtils;
import org.littleshoot.mina.common.ByteBuffer;
import org.littleshoot.mina.common.IoSession;
import org.junit.Test;
import org.lastbamboo.common.tcp.frame.TcpFrame;
import org.lastbamboo.common.tcp.frame.TcpFrameEncoder;
import org.littleshoot.util.mina.ByteBufferUtils;
import org.littleshoot.util.mina.MinaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for the class that decodes {@link TcpFrame}ed data before passing it
 * on to another delegate.
 */
public class TcpFrameTurnClientListenerTest
    {
    
    private static final int NUM_REMOTE_ADDRESSES = 10;
    private final Logger m_log = LoggerFactory.getLogger(getClass());
    
    @Test public void testTcpFrameDecoding() throws Exception
        {
        final byte[] originalData = new byte[100000];
        // Just give the data some ordering.
        for (int i = 0; i < originalData.length; i++)
            {
            originalData[i] = (byte) (i % 127);
            }
        
        // We need to store the data per remote address.
        final Map<InetSocketAddress, ByteBuffer> readAddressesToBufs =
            new HashMap<InetSocketAddress, ByteBuffer>();
        final AtomicInteger receivedMessages = new AtomicInteger(0);
        final TurnClientListener delegateListener =
            new TurnClientListener()
            {
            public void close()
                {
                }
            public void onData(final InetSocketAddress remoteAddress, 
                final IoSession session, final byte[] data)
                {
                final ByteBuffer buf;
                if (readAddressesToBufs.containsKey(remoteAddress))
                    {
                    buf = readAddressesToBufs.get(remoteAddress);
                    }
                else
                    {
                    buf = ByteBuffer.allocate(originalData.length);
                    readAddressesToBufs.put(remoteAddress, buf);
                    }
                
                buf.put(data);
                receivedMessages.incrementAndGet();
                }
            public void onRemoteAddressClosed(
                final InetSocketAddress remoteAddress)
                {
                }
            public IoSession onRemoteAddressOpened(
                final InetSocketAddress remoteAddress, final IoSession session)
                {
                return null;
                }
            };
        
        //final TurnStunDemuxableProtocolCodecFactory mapper = 
          //  new TurnStunDemuxableProtocolCodecFactory();
        // This class just decodes the TCP frames.
        final TurnClientListener turnClientListener =
            new StunTcpFrameTurnClientListener(null, delegateListener);
        
        // Here's the idea:
        // We encode a bunch of data in TCP frames.  We then combine all those
        // frames into one big byte buffer and send data along at random
        // intervals.  This ensures we don't always sent full message chunks
        // the decoder, simulating how data would really arrive from the
        // TURN server (in randomly wrapped Data Indications).
        //
        // We also do this for a bunch of different remote addresses, so each
        // remote address should receive the original message.  We do this 
        // because we need to make sure data from each remote address is
        // decoded independently.
        final Collection<InetSocketAddress> remoteAddresses =
            createRemoteAddresses();
        
        final Map<InetSocketAddress, ByteBuffer> addressesToBuffersToWrite =
            createAddressesToBuffers(remoteAddresses, originalData);
        
        final Set<Entry<InetSocketAddress, ByteBuffer>> entries = 
            addressesToBuffersToWrite.entrySet();

        while (!entries.isEmpty())
            {
            for (final Iterator<Entry<InetSocketAddress, ByteBuffer>> iter = 
                entries.iterator(); iter.hasNext();)
                {
                final Entry<InetSocketAddress, ByteBuffer> entry = iter.next();
                final ByteBuffer buf = entry.getValue();
                if (buf.remaining() == 0)
                    {
                    // We've sent all the data, so remove the entry.
                    iter.remove();
                    }
                final InetSocketAddress remoteAddress = entry.getKey();
                final int random = RandomUtils.nextInt() % 0x0fff;
                final int bytesToSend;
                if (random > buf.remaining())
                    {
                    bytesToSend = buf.remaining();
                    }
                else
                    {
                    bytesToSend = random;
                    }
                final byte[] data = new byte[bytesToSend];
                buf.get(data);
                turnClientListener.onData(remoteAddress, null, data);
                }
            }
        
        assertEquals(remoteAddresses.size(), readAddressesToBufs.keySet().size());
        
        final Collection<ByteBuffer> bufs = readAddressesToBufs.values();
        
        for (final ByteBuffer readDataBuf : bufs)
            {
            readDataBuf.flip();
            final byte[] readData = MinaUtils.toByteArray(readDataBuf);
            assertTrue("Original data not equal to read data:\n" +
                "original: " + new String(originalData)+"\n" +
                "read:     " + new String(readData), 
                Arrays.equals(originalData, readData));
            }
        
        /*
        for (final InetSocketAddress remoteAddress : remoteAddresses)
            {
            final ByteBuffer originalBuf = ByteBuffer.wrap(originalData);
            
            final Collection<byte[]> bufs = 
                MinaUtils.splitToByteArrays(originalBuf, 0xffff - 1000);
    
            final Collection<ByteBuffer> framedBufs =
                new LinkedList<ByteBuffer>();
            for (final byte[] curData : bufs)
                {
                final TcpFrame frame = new TcpFrame(curData);
                final TcpFrameEncoder encoder = new TcpFrameEncoder();
                final ByteBuffer buf = encoder.encode(frame);
                framedBufs.add(buf);
                }
            
            // Now combine it all and send it randomly.
            final ByteBuffer combined = ByteBufferUtils.combine(framedBufs);
            int dataMessagesSent = 0;
            while (combined.hasRemaining())
                {
                final int random = RandomUtils.nextInt() % 0xffff;
                final int bytesToSend;
                if (random > combined.remaining())
                    {
                    bytesToSend = combined.remaining();
                    }
                else
                    {
                    bytesToSend = random;
                    }
                final byte[] data = new byte[bytesToSend];
                combined.get(data);
                turnClientListener.onData(remoteAddress, null, data);
                
                dataMessagesSent++;
                m_log.debug("Data messages sent: {}", dataMessagesSent);
                }
    
            assertEquals(framedBufs.size(), receivedMessages.get());
            
            // Now make sure the data we read matches the original data exactly.
            final ByteBuffer readDataBuf = addressesToBufs.get(remoteAddress);
            readDataBuf.flip();
            final byte[] readData = MinaUtils.toByteArray(readDataBuf);
            assertTrue("Original data not equal to read data!!", 
                Arrays.equals(originalData, readData));
            }
            */
        }
    
    private Map<InetSocketAddress, ByteBuffer> createAddressesToBuffers(
        final Collection<InetSocketAddress> remoteAddresses, 
        final byte[] originalData)
        {
        final Map<InetSocketAddress, ByteBuffer> addressesToBuffers = 
            new HashMap<InetSocketAddress, ByteBuffer>();
        for (final InetSocketAddress remoteAddress : remoteAddresses)
            {
            final ByteBuffer originalBuf = ByteBuffer.wrap(originalData);
            
            final Collection<byte[]> bufs = 
                MinaUtils.splitToByteArrays(originalBuf, 0xffff - 1000);
    
            final Collection<ByteBuffer> framedBufs =
                new LinkedList<ByteBuffer>();
            for (final byte[] curData : bufs)
                {
                final TcpFrame frame = new TcpFrame(curData);
                final TcpFrameEncoder encoder = new TcpFrameEncoder();
                final ByteBuffer buf = encoder.encode(frame);
                framedBufs.add(buf);
                }
            
            // Now combine it all and send it randomly.
            final ByteBuffer combined = ByteBufferUtils.combine(framedBufs);
            addressesToBuffers.put(remoteAddress, combined);
            }
        return addressesToBuffers;
        }

    private static Collection<InetSocketAddress> createRemoteAddresses()
        {
        final Collection<InetSocketAddress> addresses = 
            new LinkedList<InetSocketAddress>();
        for (int i=0; i < NUM_REMOTE_ADDRESSES; i++)
            {
            addresses.add(newAddress(i));
            }
        return addresses;
        }
    
    private static InetSocketAddress newAddress(final int i)
        {
        // Just use some random addresses and port.
        final String addressBase = "47.2.97.";
        final int portBase = 2794;
        final String address = addressBase + (34 + i);
        final int port = portBase + i;
        final InetSocketAddress remoteAddress = 
            new InetSocketAddress(address, port);
        return remoteAddress;
        }
    }

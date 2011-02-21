package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.littleshoot.mina.common.ByteBuffer;
import org.littleshoot.mina.common.IoSession;
import org.littleshoot.mina.filter.codec.ProtocolCodecFactory;
import org.littleshoot.mina.filter.codec.ProtocolDecoder;
import org.littleshoot.mina.filter.codec.ProtocolDecoderOutput;
import org.lastbamboo.common.stun.stack.StunDemuxableProtocolCodecFactory;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitor;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;
import org.lastbamboo.common.tcp.frame.TcpFrame;
import org.lastbamboo.common.tcp.frame.TcpFrameCodecFactory;
import org.littleshoot.util.mina.DemuxableProtocolCodecFactory;
import org.littleshoot.util.mina.DemuxingProtocolCodecFactory;
import org.littleshoot.util.mina.MinaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that processes incoming data from a TURN client that needs to be
 * demultiplexed between TCP frames and STUN messages.
 */
public class StunTcpFrameTurnClientListener implements TurnClientListener
    {
    
    private final Logger m_log = LoggerFactory.getLogger(getClass());
    
    /**
     * We need a separate decoder for each remote address, as we can receive
     * data from multiple remote addresses simultaneously.  This maps from
     * those addresses to their respective decoders.
     */
    private final Map<InetSocketAddress, ProtocolDecoder> m_addressesToDecoders =
        new ConcurrentHashMap<InetSocketAddress, ProtocolDecoder>();
    
    private final TurnClientListener m_delegateListener;

    private final StunMessageVisitorFactory m_stunMessageVisitorFactory;
    
    private volatile int m_totalUnframedBytes = 0;

    private int m_totalDataBytesSentToDecode;
    
    /**
     * Creates a new class that decodes {@link TcpFrame}s from incoming data.
     * 
     * @param stunMessageVisitorFactory The factory for visiting STUN messages.
     * @param delegateListener The listener to forward all events to.
     * @param mapper Class that maps STUN transaction IDs to remote addresses.
     */
    public StunTcpFrameTurnClientListener(
        final StunMessageVisitorFactory stunMessageVisitorFactory,
        final TurnClientListener delegateListener) 
        {
        m_stunMessageVisitorFactory = stunMessageVisitorFactory;
        m_delegateListener = delegateListener;
        }

    public void onData(final InetSocketAddress remoteAddress, 
        final IoSession session, final byte[] data) 
        {
        m_log.debug("Received data");
        final ProtocolDecoderOutput out = new ProtocolDecoderOutput()
            {
            public void flush()
                {
                }
            public void write(final Object message) 
                {
                // Thoroughly annoying hack to reuse the demuxing IoHandlers.
                // The problem is we need to keep track of the remote address,
                // and we lose it if we simply send the message along to the
                // next handler.  
                // The next handler could or could not be this class, as it
                // could be a STUN message.
                if (TcpFrame.class.isAssignableFrom(message.getClass()))
                    {
                    final TcpFrame frame = (TcpFrame) message;
                    final byte[] unframed = frame.getData();
                    m_totalUnframedBytes += unframed.length;
                    m_log.debug("Unframed bytes: {}", m_totalUnframedBytes);
                    m_delegateListener.onData(remoteAddress, session, unframed);
                    }
                else if (StunMessage.class.isAssignableFrom(message.getClass()))
                    {
                    final StunMessage sm = (StunMessage) message;
                    //m_mapper.mapMessage(sm, remoteAddress);
                    
                    final TurnStunMessageMapper mapper =
                        (TurnStunMessageMapper) session.getAttribute(
                            "REMOTE_ADDRESS_MAP");
                    mapper.mapMessage(sm, remoteAddress);
                    
                    final StunMessageVisitor visitor = 
                        m_stunMessageVisitorFactory.createVisitor(session);
                    try
                        {
                        sm.accept(visitor);
                        }
                    catch (final Exception e)
                        {
                        m_log.error(
                            "Could not process STUN message. "+message, e);
                        }
                    }
                else
                    {
                    m_log.error("Could not recognize data: {}", message);
                    }
                }
            };
        final ByteBuffer dataBuf = ByteBuffer.wrap(data);
        final ProtocolDecoder decoder = getDecoder(remoteAddress);
        try
            {
            decoder.decode(session, dataBuf, out);
            }
        catch (final Exception e)
            {
            m_log.warn("Error decoding data: {}", 
                MinaUtils.toAsciiString(dataBuf), e);
            }
        m_totalDataBytesSentToDecode += data.length;
        m_log.debug("Total data bytes sent to decode: {}", 
            m_totalDataBytesSentToDecode);
        m_log.debug("Processed data...");
        }

    private ProtocolDecoder getDecoder(final InetSocketAddress remoteAddress)
        {
        if (this.m_addressesToDecoders.containsKey(remoteAddress))
            {
            return this.m_addressesToDecoders.get(remoteAddress);
            }
        else
            {
            final ProtocolDecoder decoder = newDecoder();
            this.m_addressesToDecoders.put(remoteAddress, decoder);
            return decoder;
            }
        }

    private ProtocolDecoder newDecoder()
        {
        final DemuxableProtocolCodecFactory stunCodecFactory =
            new StunDemuxableProtocolCodecFactory();
        final DemuxableProtocolCodecFactory tcpFramingCodecFactory =
            new TcpFrameCodecFactory();
        final ProtocolCodecFactory dataCodecFactory = 
            new DemuxingProtocolCodecFactory(stunCodecFactory, 
                tcpFramingCodecFactory);
        try
            {
            return dataCodecFactory.getDecoder();
            }
        catch (final Exception e)
            {
            m_log.error("Could not create decoder", e);
            throw new RuntimeException("Could not create decoder!!", e);
            }
        }

    public IoSession onRemoteAddressOpened(
        final InetSocketAddress remoteAddress, final IoSession ioSession)
        {
        return 
            m_delegateListener.onRemoteAddressOpened(remoteAddress, ioSession);
        }

    public void onRemoteAddressClosed(final InetSocketAddress remoteAddress)
        {
        this.m_delegateListener.onRemoteAddressClosed(remoteAddress);
        }

    public void close()
        {
        this.m_delegateListener.close();
        }
    }

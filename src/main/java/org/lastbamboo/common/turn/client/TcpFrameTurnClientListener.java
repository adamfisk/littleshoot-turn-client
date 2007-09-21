package org.lastbamboo.common.turn.client;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoHandler;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.lastbamboo.common.stun.stack.StunDemuxableProtocolCodecFactory;
import org.lastbamboo.common.stun.stack.StunIoHandler;
import org.lastbamboo.common.stun.stack.message.StunMessage;
import org.lastbamboo.common.stun.stack.message.StunMessageVisitorFactory;
import org.lastbamboo.common.tcp.frame.TcpFrame;
import org.lastbamboo.common.tcp.frame.TcpFrameCodecFactory;
import org.lastbamboo.common.util.mina.DemuxableProtocolCodecFactory;
import org.lastbamboo.common.util.mina.DemuxingProtocolCodecFactory;
import org.lastbamboo.common.util.mina.MinaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that decodes {@link TcpFrame} data from TURN Data Indication messages.
 * All events are forwarded to the delegate listener.  This class, however,
 * uses the decorator pattern to decode {@link TcpFrame} messages and to
 * forward the raw encapsulated data on to the next delegate.<p>
 * 
 * When this class receives STUN messages instead of {@link TcpFrame} messages,
 * it forwards the STUN messages to the specified visitor.
 */
public class TcpFrameTurnClientListener implements TurnClientListener
    {
    
    private final Logger m_log = LoggerFactory.getLogger(getClass());
    
    private final Map<InetSocketAddress, ProtocolDecoder> m_addressesToDecoders =
        new ConcurrentHashMap<InetSocketAddress, ProtocolDecoder>();
    
    private final TurnClientListener m_delegateListener;

    private final StunMessageVisitorFactory<StunMessage> 
        m_stunMessageVisitorFactory;
    
    private volatile int m_totalUnframedBytes = 0;
    
    /**
     * Creates a new class that decodes {@link TcpFrame}s from incoming data.
     * 
     * @param stunMessageVisitorFactory The factory for visiting STUN messages.
     * @param delegateListener The listener to forward all events to.
     */
    public TcpFrameTurnClientListener(
        final StunMessageVisitorFactory<StunMessage> stunMessageVisitorFactory,
        final TurnClientListener delegateListener) 
        {
        m_stunMessageVisitorFactory = stunMessageVisitorFactory;
        m_delegateListener = delegateListener;
        }

    private int m_totalDataBytesSentToDecode;
    
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
                    final IoHandler stunIoHandler = 
                        new StunIoHandler<StunMessage>(
                            m_stunMessageVisitorFactory);
                    try
                        {
                        stunIoHandler.messageReceived(session, message);
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

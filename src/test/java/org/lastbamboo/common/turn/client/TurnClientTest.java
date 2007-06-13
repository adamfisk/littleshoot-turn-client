package org.lastbamboo.common.turn.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

import org.apache.commons.id.uuid.UUID;
import org.apache.mina.common.ByteBuffer;
import org.lastbamboo.client.util.settings.HttpSettings;
import org.lastbamboo.common.stun.stack.encoder.StunMessageEncoder;
import org.lastbamboo.common.stun.stack.message.StunMessageType;
import org.lastbamboo.common.stun.stack.message.attributes.StunAttributeType;
import org.lastbamboo.common.stun.stack.message.turn.DataIndication;
import org.lastbamboo.common.stun.stack.message.turn.SuccessfulAllocateResponse;
import org.lastbamboo.common.util.ConnectionMaintainerListener;
import org.lastbamboo.common.util.mina.MinaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

/**
 * Test for the TURN client.
 */
public class TurnClientTest extends TestCase
    {
    
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    
    private final String m_httpRequestLine = "GET /test HTTP/1.1\r\n";
    
    private final String m_httpResponseLine = "HTTP/1.1 200 OK\r\n";
    
    private boolean m_connected = false;

    private boolean m_serverGotHttpRequest = false;
    
    private boolean m_turnServerGotHttpResponse = false;
    
    private boolean m_httpFailed = true;
    private boolean m_addressFailed = true;

    public void testConnectsToHttpServer() throws Exception
        {
        startThreadedHttpServer();
        startThreadedTurnServer();
        final ConnectionMaintainerListener<InetSocketAddress> listener =
            new ConnectionMaintainerListener<InetSocketAddress>()
            {

            public void connected(final InetSocketAddress server)
                {
                synchronized (TurnClientTest.this)
                    {
                    m_connected = true;
                    TurnClientTest.this.notify();
                    }
                }

            public void connectionFailed()
                {
                fail("Could not get connection.");
                synchronized (TurnClientTest.this)
                    {
                    m_connected = false;
                    TurnClientTest.this.notify();
                    }
                }

            public void disconnected()
                {
                fail("Disconnected.");
                }

            public void reconnected()
                {
                
                }
            
            };
        final InetSocketAddress localServer = 
            new InetSocketAddress("127.0.0.1", 3478);
        final TurnClient client = 
            new TurnClientImpl(listener, localServer);
        client.connect();
        
        synchronized (this)
            {
            if (!m_connected)
                {
                wait(1600);
                }
            }
        
        // This means we got the allocate response.
        assertTrue(this.m_connected);
        
        synchronized (this)
            {
            if (!m_serverGotHttpRequest)
                {
                wait(1600);
                }
            }
        
        assertTrue(m_serverGotHttpRequest);
        
        // Now make sure the TURN server got the HTTP response wrapped in a
        // Send Indication message.
        synchronized (this)
            {
            if (!this.m_turnServerGotHttpResponse)
                {
                wait(1600);
                }
            }
        
        assertTrue(this.m_turnServerGotHttpResponse);
        assertFalse(this.m_addressFailed);
        assertFalse(this.m_httpFailed);
        }

    private void startThreadedHttpServer()
        {
        final Runnable serverRunner = new Runnable()
            {
            public void run()
                {
                try
                    {
                    startHttpServer();
                    }
                catch (Exception e)
                    {
                    TurnClientTest.fail("Server errror: "+e.getMessage());
                    }
                }
            };
            
        final Thread serverThread = 
            new Thread(serverRunner, "Test-HTTP-Thread");
        serverThread.setDaemon(true);
        serverThread.start();
        }
    
    private void startThreadedTurnServer()
        {
        final Runnable serverRunner = new Runnable()
            {
            public void run()
                {
                try
                    {
                    startTurnServer();
                    }
                catch (Exception e)
                    {
                    LOG.error("Could not start server", e);
                    TurnClientTest.fail("Server errror: "+e.getMessage());
                    }
                }
            };
            
        final Thread serverThread = 
            new Thread(serverRunner, "Test-TURN-Server-Thread");
        serverThread.setDaemon(true);
        serverThread.start();
        }
    
    private void startTurnServer() throws Exception
        {
        final ServerSocket server = new ServerSocket(3478);
        LOG.debug("About to wait...");
        final Socket client = server.accept();
        LOG.debug("Got socket...");
        final InputStream is = client.getInputStream();
        final byte[] header = new byte[20];
        final int read = is.read(header);
        assertEquals(20, read);
        final ByteBuffer allocateRequestBuffer = ByteBuffer.allocate(20);
        allocateRequestBuffer.put(header);
        allocateRequestBuffer.flip();
        final int messageType = allocateRequestBuffer.getUnsignedShort();
        assertEquals(StunMessageType.ALLOCATE_REQUEST, messageType);
        final int messageLength = allocateRequestBuffer.getUnsignedShort();
        assertEquals(0, messageLength);
        final byte[] transactionId = new byte[16];
        allocateRequestBuffer.get(transactionId);
        LOG.debug("Got trans ID");
        
        final OutputStream os = client.getOutputStream();
        final InetSocketAddress random = new InetSocketAddress(42314);
        final SuccessfulAllocateResponse sar = 
            new SuccessfulAllocateResponse(new UUID(transactionId), random);
        final StunMessageEncoder encoder = new StunMessageEncoder();
        final ByteBuffer encodedResponse = encoder.encode(sar);
        os.write(MinaUtils.toByteArray(encodedResponse));
        
        // Now write a wrapped HTTP request.
        final InetSocketAddress remoteAddress = 
            new InetSocketAddress(27943);
        final DataIndication indication = 
            new DataIndication(remoteAddress, 
                m_httpRequestLine.getBytes("US-ASCII"));
        final ByteBuffer indicationBuf = encoder.encode(indication);
        os.write(MinaUtils.toByteArray(indicationBuf));
        os.flush();
        
        // Now read the HTTP response packaged in a Send Indication.
        final int sendIndicationSize = 
            20 + 12 + 4 + this.m_httpResponseLine.length();
        final byte[] sendIndicationBytes = new byte[sendIndicationSize];
        
        is.read(sendIndicationBytes);
        LOG.debug("Read send indication bytes...");
        final ByteBuffer sendBuf = ByteBuffer.wrap(sendIndicationBytes);
        final int sendType = sendBuf.getUnsignedShort();
        assertEquals(StunMessageType.SEND_INDICATION, sendType);
        final int sendLength = sendBuf.getUnsignedShort();
        assertEquals(16 + this.m_httpResponseLine.length(), sendLength);
        LOG.debug("Got expected length");
        final byte[] sendTransIdBytes = new byte[16];
        sendBuf.get(sendTransIdBytes);
        
        final int firstAttributeType = sendBuf.getUnsignedShort();
        final int firstAttributeLength = sendBuf.getUnsignedShort();
        final byte[] firstAttributeBody = new byte[firstAttributeLength];
        sendBuf.get(firstAttributeBody);
        
        final int secondAttributeType = sendBuf.getUnsignedShort();
        final int secondAttributeLength = sendBuf.getUnsignedShort();
        final byte[] secondAttributeBody = new byte[secondAttributeLength];
        sendBuf.get(secondAttributeBody);
        
        
        if (firstAttributeType == StunAttributeType.DATA)
            {
            m_httpFailed = checkHttpResponse(firstAttributeBody);
            }
        else if (secondAttributeType == StunAttributeType.DATA)
            {
            m_httpFailed = checkHttpResponse(secondAttributeBody);
            }
        else
            {
            fail("Did not receive attribute body");
            m_httpFailed = true;
            }
        
        if (firstAttributeType == StunAttributeType.REMOTE_ADDRESS)
            {
            // Make sure we get the same remote address sent in our
            // data indication.
            m_addressFailed = checkRemoteAddress(remoteAddress, firstAttributeBody);
            }
        else if (secondAttributeType == StunAttributeType.REMOTE_ADDRESS)
            {
            // Make sure we get the same remote address sent in our
            // data indication.
            m_addressFailed = 
                checkRemoteAddress(remoteAddress, secondAttributeBody);
            }
        else
            {
            fail("Did not get address");
            m_addressFailed = true;
            }
        
        m_turnServerGotHttpResponse = true;
        synchronized (this)
            {
            notify();
            }
        }

    private boolean checkRemoteAddress(final InetSocketAddress remoteAddress, 
        final byte[] body) throws Exception
        {
        final ByteBuffer buf = ByteBuffer.wrap(body);
        final int family = buf.getUnsignedShort();
        assertEquals("Expected IPv4", 0x01, family);
        final int port = buf.getUnsignedShort();
        assertEquals(remoteAddress.getPort(), port);
        
        final byte[] addressBytes = new byte[4];
        buf.get(addressBytes);
        final InetAddress address = InetAddress.getByAddress(addressBytes);
        assertEquals(remoteAddress.getAddress(), address);
        return !remoteAddress.getAddress().equals(address);
        }

    private boolean checkHttpResponse(final byte[] body) throws Exception
        {
        final String httpResponse = new String(body, "US-ASCII");
        assertEquals(this.m_httpResponseLine, httpResponse);
        LOG.debug("Got HTTP response!!!");
        return !this.m_httpResponseLine.equals(httpResponse);
        }

    private void startHttpServer() throws Exception
        {
        final ServerSocket server = 
            new ServerSocket(HttpSettings.HTTP_PORT.getValue());
        final Socket client = server.accept();
        final InputStream is = client.getInputStream();
        final Scanner scan = new Scanner(is);
        scan.useDelimiter("\r\n");
        final String request = scan.next();
        LOG.debug("Got request: "+request);
        assertEquals(m_httpRequestLine.trim(), request);
        synchronized (this)
            {
            m_serverGotHttpRequest = true;
            notify();
            }
        
        // Now, write the HTTP response and make sure our fake TURN server
        // gets it in a Send Indication!!  Note it doesn't matter at all that
        // this is HTTP, hence the bare minimum messages here.
        final OutputStream os  = client.getOutputStream();
        
        os.write(m_httpResponseLine.getBytes("US-ASCII"));
        os.flush();
        }
    }

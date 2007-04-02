package org.lastbamboo.shoot.turn.client.stub;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public final class SocketChannelStub extends SocketChannel
    {

    public SocketChannelStub()
        {
        super(new SelectorProviderStub());
        }

    public boolean finishConnect() throws IOException
        {
        // TODO Auto-generated method stub
        return false;
        }

    public boolean isConnected()
        {
        // TODO Auto-generated method stub
        return false;
        }

    public boolean isConnectionPending()
        {
        // TODO Auto-generated method stub
        return false;
        }

    public Socket socket()
        {
        // TODO Auto-generated method stub
        return null;
        }

    public boolean connect(SocketAddress arg0) throws IOException
        {
        // TODO Auto-generated method stub
        return false;
        }

    public int read(ByteBuffer arg0) throws IOException
        {
        // TODO Auto-generated method stub
        return 0;
        }

    public int write(ByteBuffer arg0) throws IOException
        {
        // TODO Auto-generated method stub
        return 0;
        }

    public long read(ByteBuffer[] arg0, int arg1, int arg2) throws IOException
        {
        // TODO Auto-generated method stub
        return 0;
        }

    public long write(ByteBuffer[] arg0, int arg1, int arg2) throws IOException
        {
        // TODO Auto-generated method stub
        return 0;
        }

    protected void implCloseSelectableChannel() throws IOException
        {
        // TODO Auto-generated method stub

        }

    protected void implConfigureBlocking(boolean arg0) throws IOException
        {
        // TODO Auto-generated method stub

        }

    }

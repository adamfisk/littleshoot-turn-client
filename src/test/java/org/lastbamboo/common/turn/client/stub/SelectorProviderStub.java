package org.lastbamboo.common.turn.client.stub;

import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

public final class SelectorProviderStub extends SelectorProvider
    {

    public DatagramChannel openDatagramChannel() throws IOException
        {
        // TODO Auto-generated method stub
        return null;
        }

    public Pipe openPipe() throws IOException
        {
        // TODO Auto-generated method stub
        return null;
        }

    public ServerSocketChannel openServerSocketChannel() throws IOException
        {
        // TODO Auto-generated method stub
        return null;
        }

    public SocketChannel openSocketChannel() throws IOException
        {
        // TODO Auto-generated method stub
        return null;
        }

    public AbstractSelector openSelector() throws IOException
        {
        // TODO Auto-generated method stub
        return null;
        }

    }

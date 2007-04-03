package org.lastbamboo.shoot.turn.client;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lastbamboo.common.protocol.CloseListener;
import org.lastbamboo.common.protocol.ReaderWriter;
import org.lastbamboo.common.protocol.ReaderWriterUtils;

/**
 * Class that keeps track of <code>ReaderWriter</code>s to the locally running
 * server that wraps and unwraps messages for exchanging data with a TURN
 * server.
 */
public final class TurnReaderWriterTrackerImpl 
    implements TurnReaderWriterTracker, CloseListener
    {
    
    /**
     * Logger for this class.
     */
    private static final Log LOG = 
        LogFactory.getLog(TurnReaderWriterTrackerImpl.class);
    
    /**
     * <code>Map</code> of remote addresses to <code>ReaderWriter</code>s 
     * for processing data from those hosts.
     */
    private final Map m_remoteAddressesToReaderWriters = 
        Collections.synchronizedMap(new HashMap());

    public boolean hasReaderWriter(final InetSocketAddress remoteAddress)
        {
        LOG.trace("Checking for reader/writer for " + remoteAddress + 
            " among " + this.m_remoteAddressesToReaderWriters.size() + 
            " candidates...");
        return this.m_remoteAddressesToReaderWriters.containsKey(remoteAddress);
        }

    public void addReaderWriter(final InetSocketAddress remoteAddress, 
        final ReaderWriter readerWriter)
        {
        LOG.trace("Adding reader/writer for: " + remoteAddress);
        this.m_remoteAddressesToReaderWriters.put(remoteAddress, readerWriter);
        readerWriter.addCloseListener(this);
        }

    public ReaderWriter getReaderWriter(final InetSocketAddress remoteAddress)
        {
        return (ReaderWriter) this.m_remoteAddressesToReaderWriters.get(
            remoteAddress);
        }

    public void removeReaderWriter(final InetSocketAddress remoteAddress)
        {
        LOG.trace("Removing reader/writer: " + remoteAddress);
        this.m_remoteAddressesToReaderWriters.remove(remoteAddress);
        }
    
    public void onClose(final ReaderWriter readerWriter)
        {
        LOG.debug("Closing ReaderWriter: "+readerWriter);
        ReaderWriterUtils.removeFromMapValues(
            this.m_remoteAddressesToReaderWriters, readerWriter);
        }
    
    }

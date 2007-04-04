package org.lastbamboo.common.turn.client;

import java.nio.ByteBuffer;

/**
 * Interface for a reader of <code>ByteBuffer</code> data.
 */
public interface ByteBufferReader
    {

    /**
     * Reads data from the given source buffer into the implementing class.
     * @param src The source buffer to read from.
     */
    void put(final ByteBuffer src);
    }

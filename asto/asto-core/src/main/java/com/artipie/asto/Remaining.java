/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import java.nio.ByteBuffer;

/**
 * Remaining bytes in a byte buffer.
 * @since 0.13
 */
public final class Remaining {

    /**
     * The buffer.
     */
    private final ByteBuffer buf;

    /**
     * Restore buffer position.
     */
    private final boolean restore;

    /**
     * Ctor.
     * @param buf The byte buffer.
     */
    public Remaining(final ByteBuffer buf) {
        this(buf, false);
    }

    /**
     * Ctor.
     * @param buf The byte buffer.
     * @param restore Restore position.
     */
    public Remaining(final ByteBuffer buf, final boolean restore) {
        this.buf = buf;
        this.restore = restore;
    }

    /**
     * Obtain remaining bytes.
     * <p>
     * Read all remaining bytes from the buffer and reset position back after
     * reading.
     * </p>
     * @return Remaining bytes.
     */
    public byte[] bytes() {
        final byte[] bytes = new byte[this.buf.remaining()];
        if (this.restore) {
            this.buf.mark();
        }
        this.buf.get(bytes);
        if (this.restore) {
            this.buf.reset();
        }
        return bytes;
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.misc;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * ByteBuffer accumulator.
 *
 * @implNote This class is not thread safe
 * @since 1.0
 */
@NotThreadSafe
@SuppressWarnings("PMD.TooManyMethods")
public final class BufAccumulator implements ReadableByteChannel, WritableByteChannel {

    /**
     * Buffer.
     */
    private ByteBuffer buffer;

    /**
     * Create buffer with initial capacity.
     *
     * @param cap Initial capacity
     */
    public BufAccumulator(final int cap) {
        this.buffer = BufAccumulator.newEmptyBuffer(cap);
    }

    /**
     * Is empty.
     * @return True if empty
     */
    public boolean empty() {
        return this.buffer.position() == 0;
    }

    /**
     * Copy buffer range to another buffer.
     *
     * @param pos Buffer position
     * @param lim Buffer limit
     * @return Readonly copy
     */
    public ByteBuffer copyRange(final int pos, final int lim) {
        final ByteBuffer src = this.duplicate();
        src.limit(lim);
        src.position(pos);
        final ByteBuffer slice = src.slice();
        final ByteBuffer res = ByteBuffer.allocate(slice.remaining());
        res.put(slice);
        res.flip();
        return res.asReadOnlyBuffer();
    }

    /**
     * Drop first n bytes.
     * <p>
     * This operation may change the duplicated buffers or other references to this buffer.
     * </p>
     *
     * @param size How many bytes to drop
     */
    public void drop(final int size) {
        this.buffer.position(size);
        this.buffer.compact();
        this.buffer.limit(this.buffer.position());
    }

    /**
     * Get a duplicate of the buffer.
     * <p>
     * It uses same shared memory as origin buffer but creates new
     * position and limit parameters.
     * </p>
     *
     * @return Duplciated buffer
     */
    public ByteBuffer duplicate() {
        this.check();
        return this.buffer.duplicate();
    }

    @Override
    public int read(final ByteBuffer dst) {
        this.check();
        int res = -1;
        if (this.buffer.position() > 0) {
            final ByteBuffer src = this.buffer.duplicate();
            src.rewind();
            final int rem = Math.min(src.remaining(), dst.remaining());
            src.limit(rem);
            dst.put(src);
            this.buffer.position(rem);
            this.buffer.compact();
            this.buffer.limit(this.buffer.position());
            res = rem;
        }
        return res;
    }

    @Override
    public int write(final ByteBuffer src) {
        this.check();
        final int size = src.remaining();
        if (this.buffer.capacity() - this.buffer.limit() >= size) {
            this.buffer.limit(this.buffer.limit() + src.remaining());
            this.buffer.put(src);
        } else {
            final int cap = Math.max(this.buffer.capacity(), src.capacity()) * 2;
            final ByteBuffer resized = ByteBuffer.allocate(cap);
            final int pos = this.buffer.position();
            final int lim = this.buffer.limit();
            this.buffer.flip();
            resized.put(this.buffer);
            resized.limit(lim + size);
            resized.position(pos);
            resized.put(src);
            this.buffer = resized;
        }
        return size;
    }

    /**
     * Accumulator size.
     * @return Size
     */
    public int size() {
        return this.buffer.position();
    }

    /**
     * Get byte array.
     *
     * @return Byte array from accumulator starting from the beginning to limit.
     */
    public byte[] array() {
        final ByteBuffer dup = this.duplicate();
        dup.rewind();
        final byte[] res = new byte[dup.remaining()];
        dup.get(res);
        return res;
    }

    @Override
    @SuppressWarnings("PMD.NullAssignment")
    public void close() {
        this.check();
        // @checkstyle MethodBodyCommentsCheck (1 lines)
        // assign to null means broken state, it's verified by `check` method.
        this.buffer = null;
    }

    @Override
    public boolean isOpen() {
        return this.buffer != null;
    }

    /**
     * Sanity check. Works with assertions flag enabled only.
     */
    private void check() {
        assert this.buffer != null : "tokenizer was closed";
    }

    /**
     * Creates new empty buffer with zero position and limit.
     *
     * @param cap Capacity
     * @return New buffer
     */
    private static ByteBuffer newEmptyBuffer(final int cap) {
        final ByteBuffer buf = ByteBuffer.allocate(cap);
        buf.flip();
        return buf;
    }
}

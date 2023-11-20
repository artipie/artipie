/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.misc;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * ByteBuffer tokenizer splits sequence of chunks of {@link ByteBuffer}s separated
 * by the delimiter bytes into token series of sequences of ByteBuffers,
 * and notify the receiver at the end of each token with boolean flag.
 * <p>
 * This tokenizer updated with {@code push} method, which appends the chunk
 * buffer into the buffer. The receiver should be specified in the constructor
 * and handles on next token chunk with boolean flag.
 * <br/>
 * Tokenizer can keep the intermediate buffer after push call if it can't determine
 * the end of the token. The buffer size is always fewer than delimiter length.
 * <br/>
 * When chunk stream is ended, tokenizer should be notified with {@code close}
 * method, it will flush the buffer to receiver if any temporary state exist.
 * </p>
 *
 * @implNote This class is not thread safe, the access to push and close
 *  should be synchronized externally if needed
 * @implNote The state could be broken in case of runtime exception occurs during
 *  the tokenization process, it couldn't be recovered and should not be used after failure
 * @since 1.0
 * @checkstyle MethodBodyCommentsCheck (500 lines)
 */
@NotThreadSafe
public final class ByteBufferTokenizer implements Closeable {

    /**
     * Empty read-only buffer.
     */
    private static final ByteBuffer EMPTY_BUF = ByteBuffer.allocate(0).asReadOnlyBuffer();

    /**
     * Default capacity is 4 Kb.
     */
    private static final int CAP_DEFAULT = 1024 * 4;

    /**
     * Delimiter.
     */
    private final byte[] delim;

    /**
     * Tokens receiver.
     */
    private final Receiver receiver;

    /**
     * Buffer acc.
     */
    private final BufAccumulator acc;

    /**
     * New tokenizer.
     *
     * @param receiver Tokens receiver
     * @param delim Delimiter
     */
    public ByteBufferTokenizer(final Receiver receiver, final byte[] delim) {
        this(receiver, delim, ByteBufferTokenizer.CAP_DEFAULT);
    }

    /**
     * New tokenizer with specified initial capacity of buffer.
     *
     * @param receiver Tokens receiver
     * @param delim Delimiter
     * @param cap Initial capacity
     */
    public ByteBufferTokenizer(final Receiver receiver, final byte[] delim, final int cap) {
        this.receiver = receiver;
        this.delim = Arrays.copyOf(delim, delim.length);
        this.acc = new BufAccumulator(cap);
    }

    /**
     * Push next chunk of data to tokenizer.
     *
     * @param chunk Next chunk
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    public void push(final ByteBuffer chunk) {
        if (this.acc.write(chunk) <= 0) {
            return;
        }
        final byte[] arr = this.acc.array();
        // bid is a next boundary id, offset is current offset of token + boundary
        int bid;
        int offset = 0;
        // find next boundary token with offset as `bid`
        while ((bid = indexOf(offset, arr, this.delim)) >= 0) {
            if (bid == 0) {
                // if boundary seq is a head, then next token is and empty token
                this.receiver.receive(ByteBufferTokenizer.EMPTY_BUF, true);
                // and offset for next lookup is a delimiter length
                offset = this.delim.length;
                continue;
            }
            // set bounds for next tokens from current offset to next delimiter id
            this.receiver.receive(this.acc.copyRange(offset, bid), true);
            // update offset to token position + delimiter length
            offset = bid + this.delim.length;
        }
        // if next delimiter was not found, then try to send save range of bytes to receiver,
        // since it may contain next delimiter partially, then safe range is a:
        // (buffer-length - (delimiter - 1))
        final int margin = arr.length - this.delim.length + 1;
        if (margin > 0) {
            if (offset < margin) {
                // if there are some bytes between last offset and margin, then send it
                // to the receiver and update buffer bounds
                this.receiver.receive(this.acc.copyRange(offset, margin), false);
                this.acc.drop(margin);
            } else {
                // if the offset is crossing possible delimiter margin,
                // then just reset to the offset position
                this.acc.drop(offset);
            }
        }
    }

    @Override
    @SuppressWarnings("PMD.NullAssignment")
    public void close() {
        this.flush();
        this.acc.close();
    }

    /**
     * Flush buffer, sends all remaining data to receiver.
     * This method guarantee that the receiver will be called with {@code end} param, even if
     * the buffer doesn't contain any bytes.
     */
    private void flush() {
        final ByteBuffer dup = this.acc.duplicate();
        dup.flip();
        if (dup.hasRemaining()) {
            this.receiver.receive(this.acc.copyRange(dup.position(), dup.limit()), true);
        } else {
            this.receiver.receive(ByteBufferTokenizer.EMPTY_BUF, true);
        }
    }

    /**
     * Finds index of token in array starting with offset.
     *
     * @param offset Offset to start
     * @param array Source array
     * @param token Token to find
     * @return Position of token or -1 if not found
     */
    private static int indexOf(final int offset, final byte[] array, final byte[] token) {
        int res = -1;
        // @checkstyle LocalVariableNameCheck (10 lines)
        TOP: for (int i = offset; i < array.length - token.length + 1; ++i) {
            boolean found = true;
            for (int j = 0; j < token.length; ++j) {
                if (array[i + j] != token[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                res = i;
                break TOP;
            }
        }
        return res;
    }

    /**
     * Tokenizer series receiver.
     *
     * @since 1.0
     */
    @FunctionalInterface
    public interface Receiver {

        /**
         * Receive next chunk of current token.
         *
         * @param next Chunk of current token
         * @param end True if the end of current token
         */
        void receive(ByteBuffer next, boolean end);
    }
}

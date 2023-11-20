/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */

package com.artipie.http.rs;

import com.artipie.asto.Content;
import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.headers.ContentLength;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Response with body.
 * @since 0.3
 */
public final class RsWithBody implements Response {

    /**
     * Origin response.
     */
    private final Response origin;

    /**
     * Body content.
     */
    private final Content body;

    /**
     * Decorates response with new text body.
     * @param origin Response to decorate
     * @param body Text body
     * @param charset Encoding
     */
    public RsWithBody(final Response origin, final CharSequence body, final Charset charset) {
        this(origin, ByteBuffer.wrap(body.toString().getBytes(charset)));
    }

    /**
     * Creates new response with text body.
     * @param body Text body
     * @param charset Encoding
     */
    public RsWithBody(final CharSequence body, final Charset charset) {
        this(ByteBuffer.wrap(body.toString().getBytes(charset)));
    }

    /**
     * Creates new response from byte buffer.
     * @param buf Buffer body
     */
    public RsWithBody(final ByteBuffer buf) {
        this(StandardRs.EMPTY, buf);
    }

    /**
     * Decorates origin response body with byte buffer.
     * @param origin Response
     * @param bytes Byte array
     */
    public RsWithBody(final Response origin, final byte[] bytes) {
        this(origin, ByteBuffer.wrap(bytes));
    }

    /**
     * Decorates origin response body with byte buffer.
     * @param origin Response
     * @param buf Body buffer
     */
    public RsWithBody(final Response origin, final ByteBuffer buf) {
        this(origin, new Content.From(Optional.of((long) buf.remaining()), Flowable.just(buf)));
    }

    /**
     * Creates new response with body publisher.
     * @param body Publisher
     */
    public RsWithBody(final Publisher<ByteBuffer> body) {
        this(StandardRs.EMPTY, body);
    }

    /**
     * Response with body from publisher.
     * @param origin Origin response
     * @param body Publisher
     */
    public RsWithBody(final Response origin, final Publisher<ByteBuffer> body) {
        this(origin, new Content.From(body));
    }

    /**
     * Creates new response with body content.
     *
     * @param body Content.
     */
    public RsWithBody(final Content body) {
        this(StandardRs.EMPTY, body);
    }

    /**
     * Decorates origin response body with content.
     * @param origin Response
     * @param body Content
     */
    public RsWithBody(final Response origin, final Content body) {
        this.origin = origin;
        this.body = body;
    }

    @Override
    public CompletionStage<Void> send(final Connection con) {
        return withHeaders(this.origin, this.body.size()).send(new ConWithBody(con, this.body));
    }

    @Override
    public String toString() {
        return String.format(
            "(%s: origin='%s', body='%s')",
            this.getClass().getSimpleName(),
            this.origin.toString(),
            this.body.toString()
        );
    }

    /**
     * Wrap response with headers if size provided.
     * @param origin Origin response
     * @param size Maybe size
     * @return Wrapped response
     */
    private static Response withHeaders(final Response origin, final Optional<Long> size) {
        return size.<Response>map(
            val -> new RsWithHeaders(
                origin, new Headers.From(new ContentLength(String.valueOf(val))), true
            )
        ).orElse(origin);
    }

    /**
     * Connection with body publisher.
     * @since 0.3
     */
    private static final class ConWithBody implements Connection {

        /**
         * Origin connection.
         */
        private final Connection origin;

        /**
         * Body publisher.
         */
        private final Publisher<ByteBuffer> body;

        /**
         * Ctor.
         * @param origin Connection
         * @param body Publisher
         */
        ConWithBody(final Connection origin, final Publisher<ByteBuffer> body) {
            this.origin = origin;
            this.body = body;
        }

        @Override
        public CompletionStage<Void> accept(
            final RsStatus status,
            final Headers headers,
            final Publisher<ByteBuffer> none) {
            return this.origin.accept(
                status, headers,
                Flowable.fromPublisher(this.body).map(ByteBuffer::duplicate)
            );
        }
    }
}

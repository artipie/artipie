/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.http.rq.RqHeaders;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * Content with size from headers.
 * @since 0.6
 */
public final class ContentWithSize implements Content {

    /**
     * Request body.
     */
    private final Publisher<ByteBuffer> body;

    /**
     * Request headers.
     */
    private final Iterable<Map.Entry<String, String>> headers;

    /**
     * Content with size from body and headers.
     * @param body Body
     * @param headers Headers
     */
    public ContentWithSize(final Publisher<ByteBuffer> body,
        final Iterable<Map.Entry<String, String>> headers) {
        this.body = body;
        this.headers = headers;
    }

    @Override
    public Optional<Long> size() {
        return new RqHeaders(this.headers, "content-length")
            .stream().findFirst()
            .map(Long::parseLong);
    }

    @Override
    public void subscribe(final Subscriber<? super ByteBuffer> subscriber) {
        this.body.subscribe(subscriber);
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rq.multipart;

import com.artipie.http.Headers;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * Empty part.
 * @since 1.0
 */
final class EmptyPart implements RqMultipart.Part {

    /**
     * Origin publisher.
     */
    private final Publisher<ByteBuffer> origin;

    /**
     * New empty part.
     * @param origin Publisher
     */
    EmptyPart(final Publisher<ByteBuffer> origin) {
        this.origin = origin;
    }

    @Override
    public void subscribe(final Subscriber<? super ByteBuffer> sub) {
        this.origin.subscribe(sub);
    }

    @Override
    public Headers headers() {
        return Headers.EMPTY;
    }
}

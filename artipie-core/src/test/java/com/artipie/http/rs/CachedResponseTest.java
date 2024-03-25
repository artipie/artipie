/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.rs;

import com.artipie.asto.Content;
import com.artipie.http.ResponseBuilder;
import io.reactivex.Flowable;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test for {@link CachedResponse}.
 */
class CachedResponseTest {

    @Test
    void shouldReadBodyOnFirstSend() {
        final AtomicBoolean terminated = new AtomicBoolean();
        final Flowable<ByteBuffer> publisher = Flowable.<ByteBuffer>empty()
            .doOnTerminate(() -> terminated.set(true));
        new CachedResponse(ResponseBuilder.ok().body(new Content.From(publisher)).build())
            .send((status, headers, body) -> CompletableFuture.allOf())
            .toCompletableFuture().join();
        MatcherAssert.assertThat(terminated.get(), new IsEqual<>(true));
    }

    @Test
    void shouldReplayBody() {
        final byte[] content = "content".getBytes();
        final CachedResponse cached = new CachedResponse(
            ResponseBuilder.ok().body(new Content.From(content)).build()
        );
        cached.send((status, headers, body) -> CompletableFuture.allOf())
            .toCompletableFuture().join();
        final AtomicReference<byte[]> capture = new AtomicReference<>();
        cached.send(
            (status, headers, body) -> new Content.From(body).asBytesFuture().thenAccept(capture::set)
        ).toCompletableFuture().join();
        Assertions.assertArrayEquals(content, capture.get());
    }
}

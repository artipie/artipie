/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rs;

import com.artipie.asto.Content;
import com.artipie.asto.ext.PublisherAs;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link CachedResponse}.
 *
 * @since 0.17
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class CachedResponseTest {

    @Test
    void shouldReadBodyOnFirstSend() {
        final AtomicBoolean terminated = new AtomicBoolean();
        final Flowable<ByteBuffer> publisher = Flowable.<ByteBuffer>empty()
            .doOnTerminate(() -> terminated.set(true));
        new CachedResponse(new RsWithBody(publisher)).send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(terminated.get(), new IsEqual<>(true));
    }

    @Test
    void shouldReplayBody() {
        final byte[] content = "content".getBytes();
        final CachedResponse cached = new CachedResponse(
            new RsWithBody(new Content.OneTime(new Content.From(content)))
        );
        cached.send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        final AtomicReference<byte[]> capture = new AtomicReference<>();
        cached.send(
            (status, headers, body) -> new PublisherAs(body).bytes().thenAccept(capture::set)
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(capture.get(), new IsEqual<>(content));
    }
}

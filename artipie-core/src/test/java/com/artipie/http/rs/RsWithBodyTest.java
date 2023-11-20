/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rs;

import com.artipie.asto.Content;
import com.artipie.asto.Remaining;
import com.artipie.http.Response;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.hm.RsHasHeaders;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RsWithBody}.
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
final class RsWithBodyTest {

    @Test
    void createsResponseWithStatusOkAndBody() {
        final byte[] body = "abc".getBytes();
        MatcherAssert.assertThat(
            new RsWithBody(ByteBuffer.wrap(body)),
            new ResponseMatcher(body)
        );
    }

    @Test
    void appendsBody() {
        final String body = "def";
        MatcherAssert.assertThat(
            new RsWithBody(new RsWithStatus(RsStatus.CREATED), body, StandardCharsets.UTF_8),
            new ResponseMatcher(RsStatus.CREATED, body, StandardCharsets.UTF_8)
        );
    }

    @Test
    void appendsContentSizeHeader() {
        final int size = 100;
        MatcherAssert.assertThat(
            new RsWithBody(StandardRs.EMPTY, new Content.From(new byte[size])),
            new RsHasHeaders(new Header("Content-Length", String.valueOf(size)))
        );
    }

    @Test
    void appendsContentSizeHeaderForContentBody() {
        final int size = 17;
        MatcherAssert.assertThat(
            new RsWithBody(new Content.From(new byte[size])),
            new RsHasHeaders(new ContentLength(size))
        );
    }

    @Test
    void overridesContentSizeHeader() {
        final int size = 17;
        MatcherAssert.assertThat(
            new RsWithBody(
                new RsWithHeaders(StandardRs.OK, new ContentLength(100)),
                new Content.From(new byte[size])
            ),
            new RsHasHeaders(new ContentLength(size))
        );
    }

    @Test
    void readTwice() throws Exception {
        final String body = "body";
        final Response target = new RsWithBody(body, StandardCharsets.US_ASCII);
        MatcherAssert.assertThat(
            "first attempty",
            responseBodyString(target, StandardCharsets.US_ASCII).toCompletableFuture().get(),
            new IsEqual<>(body)
        );
        MatcherAssert.assertThat(
            "second attempty",
            responseBodyString(target, StandardCharsets.US_ASCII).toCompletableFuture().get(),
            new IsEqual<>(body)
        );
    }

    /**
     * Fetch response body string.
     * @param rsp Response
     * @param charset String charset
     * @return String future
     */
    private static CompletionStage<String> responseBodyString(final Response rsp,
        final Charset charset) {
        final CompletableFuture<String> future = new CompletableFuture<>();
        return rsp.send(
            (status, headers, body) ->
                Flowable.fromPublisher(body)
                    .toList()
                    .map(
                        list -> list.stream()
                            .reduce(
                                (left, right) -> {
                                    final ByteBuffer concat = ByteBuffer.allocate(
                                        left.remaining() + right.remaining()
                                    ).put(left).put(right);
                                    concat.flip();
                                    return concat;
                                }
                            ).orElse(ByteBuffer.allocate(0))
                    ).map(buf -> new Remaining(buf).bytes())
                    .doOnSuccess(data -> future.complete(new String(data, charset)))
                    .ignoreElement().to(CompletableInterop.await())
        ).thenCompose(ignore -> future);
    }
}

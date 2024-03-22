/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.http.BaseResponse;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.slice.SliceSimple;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Test case for {@link ArtipieStorage}.
 */
public final class ArtipieStorageTest {

    @Test
    void shouldSave() throws Exception {
        final Key key = new Key.From("a", "b", "hello.txt");
        final byte[] content = "Hello world!!!".getBytes();
        final AtomicReference<RequestLine> line = new AtomicReference<>();
        final AtomicReference<Headers> headers =
            new AtomicReference<>();
        final AtomicReference<byte[]> body = new AtomicReference<>();
        new BlockingStorage(
            new ArtipieStorage(
                new FakeClientSlices(
                    (rqline, rqheaders, rqbody) -> {
                        line.set(rqline);
                        headers.set(rqheaders);
                        return new AsyncResponse(
                            new Content.From(rqbody).asBytesFuture()
                                .thenApply(
                                    bytes -> {
                                        body.set(bytes);
                                        return BaseResponse.ok();
                                    }
                                )
                        );
                    }
                ), new URI("http://host/path1")
            )
        ).save(key, content);
        MatcherAssert.assertThat(
            "Request line to save a value",
            line.get(),
            new IsEqual<>(
                new RequestLine(
                    RqMethod.PUT, String.format("/path1/%s", key)
                )
            )
        );
        MatcherAssert.assertThat(
            "Content-length header value should be equal to the content length",
            new RqHeaders(headers.get(), "content-length"),
            Matchers.contains(String.valueOf(content.length))
        );
        MatcherAssert.assertThat(
            "Request body should be equal to the content",
            body.get(),
            new IsEqual<>(content)
        );
    }

    @Test
    void shouldThrowExceptionWhenSavingIsFailed() {
        ArtipieStorageTest.assertThrowException(
            () -> new ArtipieStorage(
                new SliceSimple(BaseResponse.internalError())
            ).save(new Key.From("1"), Content.EMPTY)
        );
    }

    @Test
    void shouldDelete() throws Exception {
        final Key key = new Key.From("delkey");
        final AtomicReference<RequestLine> line = new AtomicReference<>();
        final AtomicReference<Headers> headers =
            new AtomicReference<>();
        final AtomicReference<byte[]> body = new AtomicReference<>();
        new BlockingStorage(
            new ArtipieStorage(
                new FakeClientSlices(
                    (rqline, rqheaders, rqbody) -> {
                        line.set(rqline);
                        headers.set(rqheaders);
                        return new AsyncResponse(
                            new Content.From(rqbody).asBytesFuture()
                                .thenApply(
                                    bytes -> {
                                        body.set(bytes);
                                        return BaseResponse.ok();
                                    }
                                )
                        );
                    }
                ), new URI("http://host/path2")
            )
        ).delete(key);
        Assertions.assertEquals(
            new RequestLine(RqMethod.DELETE, String.format("/path2/%s", key)),
            line.get(),
            "Request line to delete a value"
        );

        Assertions.assertTrue(headers.get().isEmpty(), "Headers are empty");
        MatcherAssert.assertThat(
            "Body is empty",
            body.get(),
            new IsEqual<>(new byte[0])
        );
    }

    @Test
    void shouldThrowExceptionWhenDeleteIsFailed() {
        ArtipieStorageTest.assertThrowException(
            () -> new ArtipieStorage(
                new SliceSimple(BaseResponse.internalError())
            ).delete(new Key.From("a"))
        );
    }

    @Test
    void shouldListKeys() {
        final Collection<Key> res = new BlockingStorage(
            new ArtipieStorage(
                new SliceSimple(
                    BaseResponse.ok().textBody("[\"a/b/file1.txt\", \"a/file2.txt\"]")
                )
            )
        ).list(new Key.From("prefix"));
        MatcherAssert.assertThat(
            res,
            Matchers.containsInAnyOrder(
                new Key.From("a", "b", "file1.txt"),
                new Key.From("a", "file2.txt")
            )
        );
    }

    @Test
    void shouldThrowExceptionWhenListIsFailed() {
        ArtipieStorageTest.assertThrowException(
            () -> new ArtipieStorage(
                new SliceSimple(BaseResponse.internalError())
            ).list(new Key.From("b"))
        );
    }

    @Test
    void shouldGetValue() {
        final String data = "test data";
        final String res = new String(
            new BlockingStorage(
                new ArtipieStorage(
                    new SliceSimple(BaseResponse.ok().textBody(data))
                )
            ).value(new Key.From("c")),
            StandardCharsets.UTF_8
        );
        Assertions.assertEquals(data, res);
    }

    @Test
    void shouldThrowExceptionWhenValueIsFailed() {
        ArtipieStorageTest.assertThrowException(
            () -> new ArtipieStorage(
                new SliceSimple(BaseResponse.internalError())
            ).value(new Key.From("key"))
        );
    }

    private static void assertThrowException(
        final Supplier<CompletableFuture<?>> supplier
    ) {
        final CompletableFuture<?> res = supplier.get();
        final Exception exception = Assertions.assertThrows(
            CompletionException.class,
            res::join
        );
        MatcherAssert.assertThat(
            "Storage 'ArtipieStorage' should fail",
            exception.getCause(),
            new IsInstanceOf(ArtipieIOException.class)
        );
    }

    /**
     * Fake {@link ClientSlices} implementation that returns specified result.
     *
     * @since 1.11.0
     */
    private static final class FakeClientSlices implements ClientSlices {

        /**
         * Slice returned by requests.
         */
        private final Slice result;

        /**
         * Ctor.
         *
         * @param result Slice returned by requests.
         */
        FakeClientSlices(final Slice result) {
            this.result = result;
        }

        @Override
        public Slice http(final String host) {
            return this.result;
        }

        @Override
        public Slice http(final String host, final int port) {
            return this.result;
        }

        @Override
        public Slice https(final String host) {
            return this.result;
        }

        @Override
        public Slice https(final String host, final int port) {
            return this.result;
        }
    }
}

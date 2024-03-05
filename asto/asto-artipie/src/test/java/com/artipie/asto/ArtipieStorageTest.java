/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.SliceSimple;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyIterable;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
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
        final AtomicReference<String> line = new AtomicReference<>();
        final AtomicReference<Iterable<Map.Entry<String, String>>> headers =
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
                                        return StandardRs.OK;
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
                ).toString()
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
                new SliceSimple(
                    new RsWithStatus(RsStatus.INTERNAL_ERROR)
                )
            ).save(new Key.From("1"), Content.EMPTY)
        );
    }

    @Test
    void shouldDelete() throws Exception {
        final Key key = new Key.From("delkey");
        final AtomicReference<String> line = new AtomicReference<>();
        final AtomicReference<Iterable<Map.Entry<String, String>>> headers =
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
                                        return StandardRs.OK;
                                    }
                                )
                        );
                    }
                ), new URI("http://host/path2")
            )
        ).delete(key);
        MatcherAssert.assertThat(
            "Request line to delete a value",
            line.get(),
            new IsEqual<>(
                new RequestLine(
                    RqMethod.DELETE, String.format("/path2/%s", key)
                ).toString()
            )
        );
        MatcherAssert.assertThat(
            "Headers are empty",
            headers.get(),
            new IsEmptyIterable<>()
        );
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
                new SliceSimple(
                    new RsWithStatus(RsStatus.INTERNAL_ERROR)
                )
            ).delete(new Key.From("a"))
        );
    }

    @Test
    void shouldListKeys() {
        final Collection<Key> res = new BlockingStorage(
            new ArtipieStorage(
                new SliceSimple(
                    new RsWithBody(
                        new Content.From(
                            "[\"a/b/file1.txt\", \"a/file2.txt\"]"
                                .getBytes(StandardCharsets.UTF_8)
                        )
                    )
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
                new SliceSimple(
                    new RsWithStatus(RsStatus.INTERNAL_ERROR)
                )
            ).list(new Key.From("b"))
        );
    }

    @Test
    void shouldGetValue() {
        final String data = "test data";
        final String res = new String(
            new BlockingStorage(
                new ArtipieStorage(
                    new SliceSimple(
                        new RsWithBody(
                            new Content.From(
                                data.getBytes(StandardCharsets.UTF_8)
                            )
                        )
                    )
                )
            ).value(new Key.From("c")),
            StandardCharsets.UTF_8
        );
        MatcherAssert.assertThat(
            res,
            new IsEqual<>(data)
        );
    }

    @Test
    void shouldThrowExceptionWhenValueIsFailed() {
        ArtipieStorageTest.assertThrowException(
            () -> new ArtipieStorage(
                new SliceSimple(
                    new RsWithStatus(RsStatus.INTERNAL_ERROR)
                )
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

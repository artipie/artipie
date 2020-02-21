/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.artipie;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.files.FilesSlice;
import com.artipie.http.Connection;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.jcabi.log.Logger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Function;
import org.cactoos.scalar.Unchecked;

/**
 * Pie of slices.
 * @since 1.0
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle ReturnCountCheck (500 lines)
 */
public final class Pie implements Slice {

    /**
     * Artipie server settings.
     */
    private final Settings settings;

    /**
     * Artipie entry point.
     * @param settings Artipie settings
     */
    public Pie(final Settings settings) {
        this.settings = settings;
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        Logger.info(this, "Request: %s", line);
        final URI uri = new RequestLineFrom(line).uri();
        if (uri.getPath().equals("*")) {
            return new RsWithStatus(200);
        }
        final String[] path = uri.getPath().replaceAll("^/+", "").split("/");
        if (uri.getPath().equals("/") || path.length == 0) {
            return new RsWithStatus(200);
        }
        final String repo = path[0];
        Logger.debug(this, "Slice repo=%s", repo);
        return new AsyncSlice(
            CompletableFuture.supplyAsync(
                () -> new Unchecked<>(() -> this.settings.storage()).value()
            )
            .thenComposeAsync(
                storage -> storage.value(new Key.From(String.format("%s.yaml", repo)))
            )
            .thenApply(content -> new RepoConfig(content))
            .thenCompose(Pie::sliceForConfig)
        ).response(line, headers, body);
    }

    /**
     * Find a slice implementation for config.
     * @param cfg Repository config
     * @return Async slice
     */
    private static CompletionStage<Slice> sliceForConfig(final RepoConfig cfg) {
        return cfg.type().thenApply(
            type -> {
                if (!"file".equals(type)) {
                    throw new IllegalStateException("We suport only `file` repo type");
                }
                final Function<Storage, Slice> func = sto -> new FilesSlice(sto);
                return func;
            }
        ).thenCombine(cfg.storage(), (factory, storage) -> factory.apply(storage));
    }

    /**
     * Async slice.
     * @since 1.0
     * @todo #12:30min Move all Async* implementation to artipie/http module.
     *  We need to wrap asynchronous slices and responses with Slice and
     *  Response interfaces.
     */
    private static final class AsyncSlice implements Slice {

        /**
         * Async slice.
         */
        private final CompletionStage<Slice> slice;

        /**
         * Ctor.
         * @param slice Async slice.
         */
        AsyncSlice(final CompletionStage<Slice> slice) {
            this.slice = slice;
        }

        @Override
        public Response response(final String line,
            final Iterable<Entry<String, String>> headers,
            final Publisher<ByteBuffer> body) {
            return new RsAsync(
                this.slice.thenApply(target -> target.response(line, headers, body))
            );
        }
    }

    /**
     * Async response.
     * @since 1.0
     */
    private static final class RsAsync implements Response {

        /**
         * Async response.
         */
        private final CompletionStage<Response> rsp;

        /**
         * Ctor.
         * @param rsp Response
         */
        RsAsync(final CompletionStage<Response> rsp) {
            this.rsp = rsp;
        }

        @Override
        public void send(final Connection con) {
            this.rsp.exceptionally(
                err -> {
                    Logger.error(Pie.class, "Unhandled error: %[exception]s", err);
                    return new RsWithBody(
                        new RsWithStatus(500),
                        err.getMessage(), StandardCharsets.UTF_8
                    );
                }
            ).thenAccept(target -> target.send(con));
        }
    }
}


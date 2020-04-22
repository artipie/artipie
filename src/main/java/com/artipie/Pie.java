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

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.composer.http.PhpComposer;
import com.artipie.files.FilesSlice;
import com.artipie.gem.GemSlice;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncSlice;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.npm.Npm;
import com.artipie.npm.http.NpmSlice;
import com.artipie.rpm.http.RpmSlice;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.cactoos.scalar.Unchecked;
import org.reactivestreams.Publisher;

/**
 * Pie of slices.
 * @since 1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ReturnCountCheck (500 lines)
 */
public final class Pie implements Slice {

    /**
     * Artipie server settings.
     */
    private final Settings settings;

    /**
     * The Vert.x instance.
     */
    private final Vertx vertx;

    /**
     * Artipie entry point.
     * @param settings Artipie settings
     * @param vertx The Vert.x instance.
     */
    public Pie(final Settings settings, final Vertx vertx) {
        this.settings = settings;
        this.vertx = vertx;
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        Logger.info(this, "Request: %s", line);
        final URI uri = new RequestLineFrom(line).uri();
        final String path = uri.getPath();
        if (path.equals("*")) {
            return new RsWithStatus(RsStatus.OK);
        }
        final String[] parts = path.replaceAll("^/+", "").split("/");
        if (path.equals("/") || parts.length == 0) {
            return new RsWithStatus(RsStatus.OK);
        }
        final String repo = parts[0];
        Logger.debug(this, "Slice repo=%s", repo);
        final MutableBoolean found = new MutableBoolean(true);
        return new AsyncSlice(
            CompletableFuture.supplyAsync(
                () -> new Unchecked<>(this.settings::storage).value()
            ).thenComposeAsync(
                storage -> storage.value(new Key.From(String.format("%s.yaml", repo)))
            ).exceptionally(
                throwable -> {
                    found.setFalse();
                    return new Content.From(new byte[] {});
                }
            ).thenCompose(
                content -> {
                    final CompletionStage<Slice> result;
                    if (found.booleanValue()) {
                        result = CompletableFuture.completedStage(
                            new RepoConfig(this.vertx, content)
                        ).thenCompose(Pie::sliceForConfig);
                    } else {
                        result = CompletableFuture.completedStage(
                            (lin, header, bdy) -> new RsWithStatus(
                                new RsWithBody(
                                    String.format("repository %s was not found", repo),
                                    StandardCharsets.UTF_8
                                ), RsStatus.NOT_FOUND
                            )
                        );
                    }
                    return result;
                }
            )
        ).response(line, headers, body);
    }

    /**
     * Find a slice implementation for config.
     * @param cfg Repository config
     * @return Async slice
     * @todo #76:30min Extract the logic in switch into separate class.
     *  It can be named like `SliceFromConfig`: it implements Slice interface
     *  and behaves as a factory by creating `Slice` instance for configuration.
     */
    private static CompletionStage<Slice> sliceForConfig(final RepoConfig cfg) {
        return cfg.type().thenCombine(
            cfg.storage(),
            (type, storage) -> {
                final CompletionStage<Slice> slice;
                switch (type) {
                    case "file":
                        slice = CompletableFuture.completedStage(new FilesSlice(storage));
                        break;
                    case "npm":
                        slice = CompletableFuture.completedStage(
                            new NpmSlice(new Npm(storage), storage)
                        );
                        break;
                    case "gem":
                        slice = CompletableFuture.completedStage(new GemSlice(storage));
                        break;
                    case "rpm":
                        slice = CompletableFuture.completedStage(new RpmSlice(storage));
                        break;
                    case "php":
                        slice = cfg.path().thenApply(
                            path -> new PhpComposer(path, storage)
                        );
                        break;
                    default:
                        throw new IllegalStateException(
                            String.format("Unsupported repository type '%s'", type)
                        );
                }
                return slice;
            }
        ).thenCompose(Function.identity());
    }
}

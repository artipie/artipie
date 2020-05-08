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
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncSlice;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.SliceSimple;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.cactoos.scalar.Unchecked;
import org.reactivestreams.Publisher;

/**
 * Pie of slices.
 * @since 1.0
 * @todo #74:30min Create a unit test for 404 response.
 *  Let's test that the response for request for not existing repository
 *  returns 404 error, e.g. if request line is `GET /repo/foo HTTP/1.1`
 *  but we don't have `foo.yml` configuration, then this class should return 404.
 * @checkstyle ReturnCountCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
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
        final Key.From key = new Key.From(String.format("%s.yaml", repo));
        return new AsyncSlice(
            CompletableFuture.supplyAsync(
                () -> new Unchecked<>(this.settings::storage).value()
            ).thenCompose(
                storage -> storage.exists(key).thenApply(
                    exist -> {
                        final Slice slice;
                        if (exist) {
                            slice = new AsyncSlice(
                                storage.value(key).thenApply(
                                    content -> new SliceFromConfig(
                                        new RepoConfig(this.vertx, content),
                                        this.vertx.fileSystem()
                                    )
                                )
                            );
                        } else {
                            slice = new SliceSimple(
                                new RsWithStatus(
                                    new RsWithBody(
                                        String.format("Repository '%s' was not found", repo),
                                        StandardCharsets.UTF_8
                                    ),
                                    RsStatus.NOT_FOUND
                                )
                            );
                        }
                        return slice;
                    }
                )
            )
        ).response(line, headers, body);
    }
}

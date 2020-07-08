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

import com.artipie.api.ArtipieApi;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.repo.FlatLayout;
import com.artipie.repo.OrgLayout;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;
import org.reactivestreams.Publisher;

/**
 * Pie of slices.
 * @since 1.0
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
        Logger.info(this, "Request: %s", line.replace("\r\n", ""));
        final URI uri = new RequestLineFrom(line).uri();
        final String path = uri.getPath();
        if (path.equals("*")) {
            return new RsWithStatus(RsStatus.OK);
        }
        final String[] parts = path.replaceAll("^/+", "").split("/");
        if (path.equals("/") || parts.length == 0) {
            return new RsWithStatus(RsStatus.NO_CONTENT);
        }
        if (path.startsWith("/css") || path.startsWith("/js")) {
            return StandardRs.NOT_FOUND;
        }
        try {
            return this.slice(line).response(line, headers, body);
        } catch (final IOException err) {
            Logger.error(this, "Failed to read settings layout: %[exception]s", err);
            return new RsWithStatus(
                new RsWithBody(
                    String.format("Failed to read Artipie settings: %s", err.getMessage()),
                    StandardCharsets.UTF_8
                ),
                RsStatus.INTERNAL_ERROR
            );
        }
    }

    /**
     * Find slice for request.
     * @param line Request line
     * @return Slice
     * @throws IOException On error
     */
    @SuppressWarnings("PMD.OnlyOneReturn")
    private Slice slice(final String line) throws IOException {
        final URI uri = new RequestLineFrom(line).uri();
        final String path = uri.getPath();
        final Slice res;
        if (path.startsWith("/api")) {
            res = new ArtipieApi(this.settings);
        } else {
            final String layout = this.settings.layout();
            if (layout == null || "flat".equals(layout)) {
                res = new FlatLayout(this.settings, this.vertx).resolve(path);
            } else if ("org".equals(layout)) {
                res = new OrgLayout(this.settings, this.vertx).resolve(path);
            } else {
                throw new IOException(String.format("Unsupported layout kind: %s", layout));
            }
            return res;
        }
        return new LoggingSlice(Level.INFO, new TrimSlice(res));
    }
}

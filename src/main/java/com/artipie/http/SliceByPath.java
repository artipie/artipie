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
package com.artipie.http;

import com.artipie.Settings;
import com.artipie.asto.Key;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Slice which finds repository by path.
 * @since 0.9
 */
final class SliceByPath implements Slice {

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * Repositories.
     */
    private final Repositories repositories;

    /**
     * New slice from settings.
     * @param settings Artipie settings
     */
    SliceByPath(final Settings settings) {
        this(settings, new ArtipieRepositories(settings));
    }

    /**
     * New slice from settings and repositories.
     * @param settings Artipie settings
     * @param repositories Repositories provider
     */
    SliceByPath(final Settings settings, final Repositories repositories) {
        this.settings = settings;
        this.repositories = repositories;
    }

    // @checkstyle ReturnCountCheck (20 lines)
    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Key key;
        try {
            final String[] split = new RequestLineFrom(line).uri().getPath()
                .replaceAll("^/+", "").split("/");
            if (this.settings.layout().equals("org")) {
                key = new Key.From(split[0], split[1]);
            } else {
                key = new Key.From(split[0]);
            }
            return this.repositories.slice(key).response(line, headers, body);
        } catch (final IOException err) {
            return new RsWithBody(
                new RsWithStatus(RsStatus.INTERNAL_ERROR),
                "Failed to parse repository config",
                StandardCharsets.UTF_8
            );
        }
    }
}

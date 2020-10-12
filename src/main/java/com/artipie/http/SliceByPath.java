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
            key = this.keyFromPath(new RequestLineFrom(line).uri().getPath());
            if (key.equals(Key.ROOT)) {
                return new RsWithBody(
                    new RsWithStatus(RsStatus.NOT_FOUND),
                    "Failed to find a repository",
                    StandardCharsets.UTF_8
                );
            }
            return this.repositories.slice(key, false).response(line, headers, body);
        } catch (final IOException err) {
            return new RsWithBody(
                new RsWithStatus(RsStatus.INTERNAL_ERROR),
                "Failed to parse repository config",
                StandardCharsets.UTF_8
            );
        }
    }

    /**
     * Key from path.
     * @param path Path from request line
     * @return Key from path.
     * @throws IOException In case of problems with reading settings.
     */
    private Key keyFromPath(final String path) throws IOException {
        final String[] split = path.replaceAll("^/+", "").split("/");
        Key key = Key.ROOT;
        if (this.settings.layout().equals("org")) {
            if (split.length >= 2) {
                key = new Key.From(split[0], split[1]);
            }
        } else {
            if (split.length >= 1) {
                key = new Key.From(split[0]);
            }
        }
        return key;
    }
}

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
package com.artipie.maven;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.RepoConfig;
import com.artipie.asto.cache.StorageCache;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.client.auth.GenericAuthenticator;
import com.artipie.maven.http.MavenProxySlice;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Maven proxy slice created from config.
 *
 * @since 0.12
 */
public final class MavenProxy implements Slice {

    /**
     * HTTP client.
     */
    private final ClientSlices client;

    /**
     * Repository configuration.
     */
    private final RepoConfig cfg;

    /**
     * Ctor.
     *
     * @param client HTTP client.
     * @param cfg Repository configuration.
     */
    public MavenProxy(final ClientSlices client, final RepoConfig cfg) {
        this.client = client;
        this.cfg = cfg;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final YamlMapping settings = this.cfg.settings()
            .orElseThrow(() -> new IllegalStateException("Repo settings missed"));
        final Authenticator auth;
        final String username = settings.string("remote_username");
        final String password = settings.string("remote_password");
        if (username == null && password == null) {
            auth = Authenticator.ANONYMOUS;
        } else {
            if (username == null) {
                throw new IllegalStateException(
                    "`username` is not specified in settings for Maven proxy"
                );
            }
            if (password == null) {
                throw new IllegalStateException(
                    "`password` is not specified in settings for Maven proxy"
                );
            }
            auth = new GenericAuthenticator(username, password);
        }
        return new MavenProxySlice(
            this.client,
            URI.create(settings.string("remote_uri")),
            auth,
            new StorageCache(this.cfg.storage())
        ).response(line, headers, body);
    }
}

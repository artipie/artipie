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
package com.artipie.docker;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.RepoConfig;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.cache.CacheDocker;
import com.artipie.docker.composite.ReadWriteDocker;
import com.artipie.docker.http.DockerSlice;
import com.artipie.docker.proxy.AuthClientSlice;
import com.artipie.docker.proxy.ClientSlices;
import com.artipie.docker.proxy.Credentials;
import com.artipie.docker.proxy.ProxyDocker;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import java.nio.ByteBuffer;
import java.util.Map;
import org.eclipse.jetty.client.HttpClient;
import org.reactivestreams.Publisher;

/**
 * Docker proxy slice created from config.
 *
 * @since 0.9
 * @todo #313:30min Unit test coverage for `DockerProxy` class
 *  `DockerProxy` class lacks unit test coverage. It should be tested that slice is built properly
 *  from configuration (e.g. handles simple response like 'GET /v2/') and fails any request for bad
 *  configuration (some required settings are missing).
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class DockerProxy implements Slice {

    /**
     * HTTP client.
     */
    private final HttpClient client;

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
    public DockerProxy(final HttpClient client, final RepoConfig cfg) {
        this.client = client;
        this.cfg = cfg;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return this.delegate().response(line, headers, body);
    }

    /**
     * Creates Docker proxy repository slice from configuration.
     *
     * @return Docker proxy slice.
     */
    private Slice delegate() {
        final YamlMapping settings = this.cfg.settings()
            .orElseThrow(() -> new IllegalStateException("Settings not found for Docker proxy"));
        final String host = settings.string("host");
        if (host == null) {
            throw new IllegalStateException("`host` is not specified in settings for Docker proxy");
        }
        final Credentials credentials;
        final String username = settings.string("username");
        final String password = settings.string("password");
        if (username == null && password == null) {
            credentials = Credentials.ANONYMOUS;
        } else {
            if (username == null) {
                throw new IllegalStateException(
                    "`username` is not specified in settings for Docker proxy"
                );
            }
            if (password == null) {
                throw new IllegalStateException(
                    "`password` is not specified in settings for Docker proxy"
                );
            }
            credentials = new Credentials.Basic(username, password);
        }
        final ClientSlices slices = new ClientSlices(this.client);
        final Docker proxy = new ProxyDocker(
            new AuthClientSlice(slices, slices.slice(host), credentials)
        );
        final Docker docker = this.cfg.storageOpt()
            .<Docker>map(
                storage -> {
                    final AstoDocker local = new AstoDocker(storage);
                    return new ReadWriteDocker(new CacheDocker(proxy, local), local);
                }
            )
            .orElse(proxy);
        return new DockerSlice(this.cfg.path(), docker);
    }
}

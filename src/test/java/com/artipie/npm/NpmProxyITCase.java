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
package com.artipie.npm;

import com.artipie.ArtipieServer;
import com.artipie.RepoConfigYaml;
import com.artipie.RepoPerms;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.rs.RsStatus;
import com.artipie.nuget.RandomFreePort;
import com.artipie.test.RepositoryUrl;
import com.artipie.test.TestContainer;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for {@link com.artipie.npm.proxy.http.NpmProxySlice}.
 * @since 0.13
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class NpmProxyITCase {

    /**
     * Project name.
     */
    private static final String PROJ = "@hello/simple-npm-project";

    /**
     * Origin repo name.
     */
    private static final String ORIGIN = "npm-origin";

    /**
     * Proxy repo name.
     */
    private static final String PROXY = "npm-proxy";

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    /**
     * Test origin.
     */
    private ArtipieServer origin;

    /**
     * Test proxy.
     */
    private ArtipieServer proxy;

    /**
     * Container.
     */
    private TestContainer cntn;

    @Test
    @Disabled
    void installFromProxy() throws Exception {
        final boolean anonymous = true;
        this.init(anonymous);
        MatcherAssert.assertThat(
            this.cntn.execStdErr(
                "npm", "install", NpmProxyITCase.PROJ,
                "--registry",
                new RepositoryUrl(this.proxy.port(), NpmProxyITCase.PROXY).string(anonymous)
            ),
            new StringContainsInOrder(
                Arrays.asList(
                    String.format("+ %s@1.0.1", NpmProxyITCase.PROJ),
                    "added 1 package"
                )
            )
        );
    }

    @Test
    void getProjectFromOriginServer() throws IOException {
        final boolean anonymous = true;
        this.init(anonymous);
        final HttpURLConnection con = (HttpURLConnection) new URL(
            String.format(
                "http://localhost:%d/%s/%s/-/%s-1.0.1.tgz",
                this.origin.port(), NpmProxyITCase.ORIGIN,
                NpmProxyITCase.PROJ, NpmProxyITCase.PROJ
            )
        ).openConnection();
        con.setRequestMethod("GET");
        con.setDoOutput(true);
        MatcherAssert.assertThat(
            con.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.OK.code()))
        );
    }

    @AfterEach
    void stop() {
        this.proxy.stop();
        this.origin.stop();
        this.cntn.close();
    }

    private void init(final boolean anonymous) throws IOException {
        this.startOrigin(anonymous);
        this.startProxy(anonymous);
        this.cntn = new TestContainer("node:14-alpine", this.tmp);
        this.cntn.start(this.proxy.port());
    }

    private void startOrigin(final boolean anonymous) throws IOException {
        final Path root = this.tmp.resolve("origin");
        root.toFile().mkdirs();
        final Path repos = root.resolve("repos");
        this.saveFilesToStrg(new FileStorage(repos));
        final int port = new RandomFreePort().value();
        this.origin = new ArtipieServer(
            root,
            NpmProxyITCase.ORIGIN,
            this.originConfig(anonymous, repos, port).toString(),
            port
        );
        this.origin.start();
    }

    private void startProxy(final boolean anonymous) throws IOException {
        final Path root = this.tmp.resolve("proxy");
        root.toFile().mkdirs();
        final int proxyport = new RandomFreePort().value();
        this.proxy = new ArtipieServer(
            root,
            NpmProxyITCase.PROXY,
            this.proxyConfig(anonymous, root, proxyport).toString(),
            proxyport
        );
        this.proxy.start();
    }

    private RepoConfigYaml originConfig(final boolean anonymous, final Path repos, final int port) {
        final RepoConfigYaml yaml = new RepoConfigYaml("npm")
            .withFileStorage(repos)
            .withUrl(String.format("http://localhost:%d/%s", port, NpmProxyITCase.ORIGIN));
        if (!anonymous) {
            yaml.withPermissions(
                new RepoPerms(ArtipieServer.ALICE.name(), "*")
            );
        }
        return yaml;
    }

    private RepoConfigYaml proxyConfig(
        final boolean anonymous, final Path root, final int proxyport
    ) {
        return new RepoConfigYaml("npm-proxy")
            .withFileStorage(root.resolve("repos"))
            .withUrl(new RepositoryUrl(proxyport, NpmProxyITCase.PROXY).string(anonymous))
            .withPath(NpmProxyITCase.PROXY)
            .withRemoteSettings(
                String.format(
                    "http://localhost:%d/%s", this.origin.port(), NpmProxyITCase.ORIGIN
                )
            );
    }

    private void saveFilesToStrg(final Storage storage) {
        new TestResource(String.format("npm/storage/%s/meta.json", NpmProxyITCase.PROJ))
            .saveTo(
                storage,
                new Key.From(NpmProxyITCase.ORIGIN, NpmProxyITCase.PROJ, "meta.json")
        );
        new TestResource(
            String.format("npm/storage/%s/-/%s-1.0.1.tgz", NpmProxyITCase.PROJ, NpmProxyITCase.PROJ)
        ).saveTo(
            storage,
            new Key.From(
                NpmProxyITCase.ORIGIN, NpmProxyITCase.PROJ, "-",
                String.format("%s-1.0.1.tgz", NpmProxyITCase.PROJ)
            )
        );
    }

}

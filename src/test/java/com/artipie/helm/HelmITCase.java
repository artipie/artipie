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
package com.artipie.helm;

import com.artipie.ArtipieServer;
import com.artipie.RepoConfigYaml;
import com.artipie.RepoPerms;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.asto.test.TestResource;
import com.artipie.http.rs.RsStatus;
import com.artipie.nuget.RandomFreePort;
import com.artipie.test.RepositoryUrl;
import com.artipie.test.TestContainer;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

/**
 * Integration tests for Helm repository.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.13
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "unchecked"})
@EnabledOnOs({OS.LINUX, OS.MAC})
final class HelmITCase {

    /**
     * Chart name.
     */
    private static final String CHART = "tomcat-0.4.1.tgz";

    /**
     * Repo name.
     */
    private static final String REPO = "my-helm";

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    /**
     * Tested Artipie server.
     */
    private ArtipieServer server;

    /**
     * Container.
     */
    private TestContainer cntn;

    /**
     * Storage.
     */
    private Storage storage;

    /**
     * Repository url.
     */
    private RepositoryUrl url;

    /**
     * Artipie server port.
     */
    private int port;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void uploadChartAndCreateIndexYaml(final boolean anonymous) throws Exception {
        this.init(anonymous);
        final HttpURLConnection con = (HttpURLConnection) new URL(
            String.format(
                "http://localhost:%s/%s/%s", this.port, HelmITCase.REPO, HelmITCase.CHART
            )
        ).openConnection();
        con.setRequestMethod("PUT");
        con.setDoOutput(true);
        if (!anonymous) {
            con.setAuthenticator(
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                            ArtipieServer.ALICE.name(),
                            ArtipieServer.ALICE.password().toCharArray()
                        );
                    }
                }
            );
        }
        ByteStreams.copy(
            new ByteArrayInputStream(
                new TestResource(String.format("helm/%s", HelmITCase.CHART)).asBytes()
            ),
            con.getOutputStream()
        );
        MatcherAssert.assertThat(
            "Response status is 200",
            con.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.OK.code()))
        );
        final Map<String, Object> index = new Yaml().load(
            new PublisherAs(
                new RxStorageWrapper(this.storage)
                    .value(new Key.From("repos", HelmITCase.REPO, "index.yaml"))
                    .blockingGet()
            ).asciiString().toCompletableFuture().join()
        );
        MatcherAssert.assertThat(
            "Version from index.yaml is correct",
            (String)
                ((ArrayList<LinkedHashMap<String, Object>>)
                    ((Map<String, Object>)
                        index.get("entries"))
                        .get("tomcat"))
                    .get(0)
                    .get("version"),
            new IsEqual<>("0.4.1")
        );
        con.disconnect();
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.cntn.close();
    }

    private void init(final boolean anonymous) throws IOException {
        this.storage = new FileStorage(this.tmp);
        this.port = new RandomFreePort().value();
        this.url = new RepositoryUrl(this.port, "my-helm");
        this.server = new ArtipieServer(
            this.tmp, "my-helm", this.config(anonymous).toString(), this.port
        );
        this.server.start();
        this.cntn = new TestContainer("bitnami/kubectl:1.19", this.tmp);
        this.cntn.start(this.port);
    }

    private RepoConfigYaml config(final boolean anonymous) {
        final RepoConfigYaml yaml = new RepoConfigYaml("helm")
            .withFileStorage(this.tmp.resolve("repos"))
            .withUrl(this.url.string(anonymous))
            .withPath(HelmITCase.REPO);
        if (!anonymous) {
            yaml.withPermissions(
                new RepoPerms(ArtipieServer.ALICE.name(), "*")
            );
        }
        return yaml;
    }

}

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
import com.artipie.npm.misc.JsonFromPublisher;
import com.artipie.nuget.RandomFreePort;
import com.artipie.test.RepositoryUrl;
import com.artipie.test.TestContainer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import javax.json.JsonObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Integration tests for Npm repository.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class NpmITCase {

    /**
     * Project name.
     */
    private static final String PROJ = "@hello/simple-npm-project";

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
     * Helper for url that may contains user credentials.
     */
    private RepositoryUrl url;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void npmInstall(final boolean anonymous) throws Exception {
        this.init(anonymous);
        this.saveFilesToStrg(NpmITCase.PROJ);
        MatcherAssert.assertThat(
            "Package was installed",
            this.cntn.execStdout(
                "npm", "install", NpmITCase.PROJ,
                "--registry", this.url.string(anonymous)
            ),
            new StringContainsInOrder(
                Arrays.asList(
                    String.format("+ %s@1.0.1", NpmITCase.PROJ),
                    "added 1 package"
                )
            )
        );
        MatcherAssert.assertThat(
            "Installed project should contain index.js",
            this.inNpmModule("index.js"),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Installed project should contain package.json",
            this.inNpmModule("package.json"),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void npmPublish(final boolean anonymous) throws Exception {
        final String tgz = String.format("%s/-/%s-1.0.1.tgz", NpmITCase.PROJ, NpmITCase.PROJ);
        final Key path = new Key.From("repos/my-npm");
        this.init(anonymous);
        new TestResource("npm/simple-npm-project")
            .addFilesTo(this.storage, new Key.From(NpmITCase.PROJ));
        MatcherAssert.assertThat(
            "Package was published",
            this.cntn.execStdout(
                "npm", "publish", NpmITCase.PROJ,
                "--registry", this.url.string(anonymous)
            ),
            new StringContains(String.format("+ %s@1.0.1", NpmITCase.PROJ))
        );
        final JsonObject meta = new JsonFromPublisher(
            this.storage.value(
                new Key.From(path, NpmITCase.PROJ, "meta.json")
            ).toCompletableFuture().join()
        ).json().toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Metadata should be valid",
            meta.getJsonObject("versions")
                .getJsonObject("1.0.1")
                .getJsonObject("dist")
                .getString("tarball"),
            new IsEqual<>(String.format("/%s", tgz))
        );
        MatcherAssert.assertThat(
            "File should be in storage after publishing",
            this.storage.exists(
                new Key.From(path, tgz)
            ).toCompletableFuture().join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void npmInstallFailWithForbidden() throws Exception {
        final ArtipieServer.User user = ArtipieServer.BOB;
        this.init(false);
        this.saveFilesToStrg(NpmITCase.PROJ);
        MatcherAssert.assertThat(
            this.cntn.execStdErr(
                "npm", "install", NpmITCase.PROJ,
                "--registry", this.url.string(user)
            ),
            new StringContains("npm ERR! 403 403 Forbidden - GET")
        );
    }

    @Test
    void npmPublishFailWithForbidden() throws Exception {
        final ArtipieServer.User user = ArtipieServer.BOB;
        this.init(false);
        new TestResource("npm/simple-npm-project")
            .addFilesTo(this.storage, new Key.From(NpmITCase.PROJ));
        MatcherAssert.assertThat(
            this.cntn.execStdErr(
                "npm", "publish", NpmITCase.PROJ,
                "--registry", this.url.string(user)
            ),
            new StringContains("npm ERR! 403 403 Forbidden - PUT")
        );
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.cntn.close();
    }

    private void init(final boolean anonymous) throws IOException {
        this.storage = new FileStorage(this.tmp);
        final int port = new RandomFreePort().value();
        this.url = new RepositoryUrl(port, "my-npm");
        this.server = new ArtipieServer(
            this.tmp, "my-npm", this.config(anonymous).toString(), port
        );
        this.server.start();
        this.cntn = new TestContainer("node:14-alpine", this.tmp);
        this.cntn.start(port);
    }

    private void saveFilesToStrg(final String proj) {
        new TestResource(String.format("npm/storage/%s/meta.json", proj))
            .saveTo(
                this.storage,
                new Key.From("repos", "my-npm", proj, "meta.json")
        );
        new TestResource(String.format("npm/storage/%s/-/%s-1.0.1.tgz", proj, proj))
            .saveTo(
                this.storage,
                new Key.From(
                    "repos", "my-npm", proj, "-", String.format("%s-1.0.1.tgz", proj)
                )
        );
    }

    private RepoConfigYaml config(final boolean anonymous) {
        final RepoConfigYaml yaml = new RepoConfigYaml("npm")
            .withFileStorage(this.tmp.resolve("repos"))
            .withUrl(this.url.string(anonymous));
        if (!anonymous) {
            yaml.withPermissions(
                new RepoPerms(ArtipieServer.ALICE.name(), "*")
            );
        }
        return yaml;
    }

    private boolean inNpmModule(final String file) {
        return this.storage.exists(
            new Key.From("node_modules", NpmITCase.PROJ, file)
        ).join();
    }

}

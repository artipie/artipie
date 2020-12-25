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
package com.artipie.pypi;

import com.artipie.ArtipieServer;
import com.artipie.RepoConfigYaml;
import com.artipie.RepoPerms;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.test.RepositoryUrl;
import com.artipie.test.TestContainer;
import java.io.IOException;
import java.nio.file.Path;
import org.cactoos.list.ListOf;
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
 * Integration tests for Pypi repository.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class PypiITCase {

    /**
     * Host.
     */
    private static final String HOST = "host.testcontainers.internal";

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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void pypiInstall(final boolean anonymous) throws Exception {
        this.init(anonymous);
        new TestResource("pypi-repo/alarmtime-0.1.5.tar.gz")
            .saveTo(
                this.storage,
                new Key.From("repos", "my-pypi", "alarmtime", "alarmtime-0.1.5.tar.gz")
        );
        MatcherAssert.assertThat(
            this.cntn.execStdout(
                "pip", "install", "--no-deps", "--trusted-host", PypiITCase.HOST,
                "--index-url", this.url.string(anonymous),
                "alarmtime==0.1.5"
            ),
            new StringContains("Successfully installed alarmtime-0.1.5")
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void pypiPublish(final boolean anonymous) throws Exception {
        this.init(anonymous);
        new TestResource("pypi-repo/example-pckg/dist/")
            .addFilesTo(
                this.storage,
                new Key.From("pypi-repo", "example-pckg")
        );
        this.cntn.execStdout("python3", "-m", "pip", "install", "--user", "--upgrade", "twine");
        MatcherAssert.assertThat(
            "Packages should be uploaded",
            this.cntn.execStdout(
                "python3", "-m", "twine", "upload", "--repository-url", this.url.string(),
                "-u", ArtipieServer.ALICE.name(), "-p", this.pswd(anonymous),
                "pypi-repo/example-pckg/*"
            ),
            new StringContainsInOrder(
                new ListOf<String>(
                    "Uploading artipietestpkg-0.0.3-py2-none-any.whl", "100%",
                    "Uploading artipietestpkg-0.0.3.tar.gz", "100%"
                )
            )
        );
        MatcherAssert.assertThat(
            "Packages should be saved in storage",
            this.inStorage("artipietestpkg-0.0.3-py2-none-any.whl")
                && this.inStorage("artipietestpkg-0.0.3.tar.gz"),
            new IsEqual<>(true)
        );
    }

    @Test
    void pypiInstallShouldFailWithForbidden() throws Exception {
        this.init(false);
        MatcherAssert.assertThat(
            this.cntn.execStdoutWithoutCheckExitCode(
                "pip", "install", "--verbose", "--no-deps", "--trusted-host", PypiITCase.HOST,
                "--index-url", this.url.string(ArtipieServer.BOB), "anypackage"
            ),
            new StringContains(
                String.format(
                    "403 Client Error: Forbidden for url: %spip/", this.url.string()
                )
            )
        );
    }

    @Test
    void pypiPublishShouldFailSinceForbidden() throws Exception {
        this.init(false);
        new TestResource("pypi-repo/example-pckg/dist/")
            .addFilesTo(
                this.storage,
                new Key.From("pypi-repo", "example-pckg")
        );
        this.cntn.execStdout("python3", "-m", "pip", "install", "--user", "--upgrade", "twine");
        MatcherAssert.assertThat(
            "Packages should not be uploaded",
            this.cntn.execStdErr(
                "python3", "-m", "twine", "upload", "--verbose",
                "--repository-url", this.url.string(),
                "-u", ArtipieServer.BOB.name(), "-p", ArtipieServer.BOB.password(),
                "pypi-repo/example-pckg/*"
            ),
            new StringContains("HTTPError: 403 Forbidden")
        );
        MatcherAssert.assertThat(
            "Packages should not be saved in storage",
            this.inStorage("artipietestpkg-0.0.3-py2-none-any.whl")
                || this.inStorage("artipietestpkg-0.0.3.tar.gz"),
            new IsEqual<>(false)
        );
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.cntn.close();
    }

    private void init(final boolean anonymous) throws IOException {
        this.storage = new FileStorage(this.tmp);
        this.server = new ArtipieServer(
            this.tmp, "my-pypi", this.config(anonymous)
        );
        final int port = this.server.start();
        this.url = new RepositoryUrl(port, "my-pypi");
        this.cntn = new TestContainer("python:3", this.tmp);
        this.cntn.start(port);
    }

    private String pswd(final boolean anonymous) {
        String pass = "pass";
        if (!anonymous) {
            pass = ArtipieServer.ALICE.password();
        }
        return pass;
    }

    private RepoConfigYaml config(final boolean anonymous) {
        final RepoConfigYaml yaml = new RepoConfigYaml("pypi")
            .withFileStorage(this.tmp.resolve("repos"));
        if (!anonymous) {
            yaml.withPermissions(
                new RepoPerms(ArtipieServer.ALICE.name(), "*")
            );
        }
        return yaml;
    }

    private boolean inStorage(final String pckg) {
        return this.storage.exists(
            new Key.From("repos", "my-pypi", "artipietestpkg", pckg)
        ).join();
    }

}

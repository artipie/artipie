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
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

/**
 * Integration tests for Pypi repository.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.12
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
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
    private GenericContainer<?> cntn;

    /**
     * Storage.
     */
    private Storage storage;

    /**
     * URL 'http://host:port/my-pypi/'.
     */
    private String url;

    /**
     * Artipie server port.
     */
    private int port;

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
            this.exec(
                "pip", "install", "--no-deps", "--trusted-host", PypiITCase.HOST,
                "--index-url", this.urlCntn(this.userOpt(anonymous)), "alarmtime==0.1.5"
            ),
            Matchers.containsString("Successfully installed alarmtime-0.1.5")
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
        this.exec("python3", "-m", "pip", "install", "--user", "--upgrade", "twine");
        MatcherAssert.assertThat(
            "Packages should be uploaded",
            this.exec(
                "python3", "-m", "twine", "upload", "--repository-url", this.url,
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
            this.exec(
                "pip", "install", "--verbose", "--no-deps", "--trusted-host", PypiITCase.HOST,
                "--index-url", this.urlCntn(Optional.of(ArtipieServer.BOB)), "anypackage"
            ),
            new StringContains(
                String.format(
                    "403 Client Error: Forbidden for url: %spip/", this.url
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
        this.exec("python3", "-m", "pip", "install", "--user", "--upgrade", "twine");
        MatcherAssert.assertThat(
            "Packages should not be uploaded",
            this.exec(
                "python3", "-m", "twine", "upload", "--verbose", "--repository-url", this.url,
                "-u", ArtipieServer.BOB.name(), "-p", ArtipieServer.BOB.password(),
                "pypi-repo/example-pckg/*"
            ),
            new IsNot<>(
                new StringContainsInOrder(
                    new ListOf<String>(
                        "Uploading artipietestpkg-0.0.3-py2-none-any.whl", "100%",
                        "Uploading artipietestpkg-0.0.3.tar.gz", "100%"
                    )
                )
            )
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
        this.cntn.stop();
    }

    private void init(final boolean anonymous) throws IOException {
        this.storage = new FileStorage(this.tmp);
        this.server = new ArtipieServer(
            this.tmp, "my-pypi", this.config(anonymous)
        );
        this.port = this.server.start();
        this.url = String.format("http://%s:%d/my-pypi/", PypiITCase.HOST, this.port);
        Testcontainers.exposeHostPorts(this.port);
        this.cntn = new GenericContainer<>("python:3")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
    }

    private String exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s", String.join(" ", command));
        return this.cntn.execInContainer(command).getStdout();
    }

    private String pswd(final boolean anonymous) {
        String pass = "pass";
        if (!anonymous) {
            pass = ArtipieServer.ALICE.password();
        }
        return pass;
    }

    private String urlCntn(final Optional<ArtipieServer.User> user) {
        final String urlcntn;
        if (user.isEmpty()) {
            urlcntn = this.url;
        } else {
            urlcntn = String.format(
                "http://%s:%s@%s:%d/my-pypi/",
                user.get().name(), user.get().password(), PypiITCase.HOST, this.port
            );
        }
        return urlcntn;
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

    private Optional<ArtipieServer.User> userOpt(final boolean anonymous) {
        final Optional<ArtipieServer.User> user;
        if (anonymous) {
            user = Optional.empty();
        } else {
            user = Optional.of(ArtipieServer.ALICE);
        }
        return user;
    }
}

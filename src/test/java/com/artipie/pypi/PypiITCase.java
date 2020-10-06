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
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

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
    private GenericContainer<?> cntn;

    /**
     * Storage.
     */
    private Storage storage;

    /**
     * URL 'http://host:port/my-pypi/'.
     */
    private String url;

    @BeforeEach
    void init() throws IOException, InterruptedException {
        this.storage = new FileStorage(this.tmp);
        this.server = new ArtipieServer(
            this.tmp, "my-pypi",
            new RepoConfigYaml("pypi")
                .withFileStorage(this.tmp.resolve("repos"))
        );
        final int port = this.server.start();
        this.url = String.format("http://%s:%d/my-pypi/", PypiITCase.HOST, port);
        Testcontainers.exposeHostPorts(port);
        this.cntn = new GenericContainer<>("python:3")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
    }

    @Test
    void pypiInstall() throws Exception {
        new TestResource("pypi-repo/alarmtime-0.1.5.tar.gz")
            .saveTo(
                this.storage,
                new Key.From("repos", "my-pypi", "alarmtime", "alarmtime-0.1.5.tar.gz")
        );
        MatcherAssert.assertThat(
            this.exec(
                "pip", "install", "--no-deps", "--trusted-host", PypiITCase.HOST,
                "--index-url", this.url, "alarmtime==0.1.5"
            ),
            Matchers.containsString("Successfully installed alarmtime-0.1.5")
        );
    }

    @Test
    void pypiPublish() throws Exception {
        this.prepareDirectory("pypi-repo/example-pckg/dist");
        this.exec("python3", "-m", "pip", "install", "--user", "--upgrade", "twine");
        MatcherAssert.assertThat(
            this.exec(
                "python3", "-m", "twine", "upload", "--repository-url", this.url, "-u",
                "any", "-p", "pass", "pypi-repo/example-pckg/dist/*"
            ),
            new StringContainsInOrder(
                new ListOf<String>(
                    "Uploading artipietestpkg-0.0.3-py2-none-any.whl", "100%",
                    "Uploading artipietestpkg-0.0.3.tar.gz", "100%"
                )
            )
        );
        MatcherAssert.assertThat(
            this.inStorage("artipietestpkg-0.0.3-py2-none-any.whl")
                && this.inStorage("artipietestpkg-0.0.3.tar.gz"),
            new IsEqual<>(true)
        );
    }

    @AfterEach
    void release() {
        this.server.stop();
        this.cntn.stop();
    }

    private String exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s", String.join(" ", command));
        return this.cntn.execInContainer(command).getStdout();
    }

    private void prepareDirectory(final String dist) throws IOException {
        FileUtils.copyDirectory(
            new TestResource(dist).asPath().toFile(),
            this.tmp.resolve(dist).toFile()
        );
    }

    private boolean inStorage(final String pckg) {
        return this.storage.exists(
            new Key.From("repos", "my-pypi", "artipietestpkg", pckg)
        ).join();
    }
}

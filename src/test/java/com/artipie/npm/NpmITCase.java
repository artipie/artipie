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
import com.jcabi.log.Logger;
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
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

/**
 * Integration tests for Npm repository.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class NpmITCase {

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
     * URL 'http://host:port/my-npm/'.
     */
    private String url;

    /**
     * Artipie server port.
     */
    private int port;

    @Test
    void npmInstall() throws Exception {
        final boolean anonymous = true;
        final String proj = "@hello/simple-npm-project";
        this.init(anonymous);
        this.saveFilesToStrg(proj);
        MatcherAssert.assertThat(
            this.exec("npm", "install", proj, "--registry", this.url),
            new StringContainsInOrder(
                Arrays.asList(
                    String.format("+ %s@1.0.1", proj),
                    "added 1 package"
                )
            )
        );
        MatcherAssert.assertThat(
            "Installed project should contain index.js",
            this.inNpmModule(proj, "index.js"),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Installed project should contain package.json",
            this.inNpmModule(proj, "package.json"),
            new IsEqual<>(true)
        );
    }

    @Test
    void npmPublish() throws Exception {
        final boolean anonymous = true;
        final String proj = "@hello/simple-npm-project";
        final Key path = new Key.From("repos/my-npm");
        this.init(anonymous);
        new TestResource("npm/simple-npm-project")
            .addFilesTo(this.storage, new Key.From(proj));
        MatcherAssert.assertThat(
            "Package was published",
            this.exec("npm", "publish", proj, "--registry", this.url),
            new StringContains("+ @hello/simple-npm-project@1.0.1")
        );
        final JsonObject meta = new JsonFromPublisher(
            this.storage.value(
                new Key.From(path, proj, "meta.json")
            ).toCompletableFuture().join()
        ).json().toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Metadata should be valid",
            meta.getJsonObject("versions")
                .getJsonObject("1.0.1")
                .getJsonObject("dist")
                .getString("tarball"),
            new IsEqual<>(String.format("/%s/-/%s-1.0.1.tgz", proj, proj))
        );
        MatcherAssert.assertThat(
            "File should be in storage after publishing",
            this.storage.exists(
                new Key.From(path, String.format("%s/-/%s-1.0.1.tgz", proj, proj))
            ).toCompletableFuture().join(),
            new IsEqual<>(true)
        );
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.cntn.stop();
    }

    private void init(final boolean anonymous) throws IOException {
        this.storage = new FileStorage(this.tmp);
        this.port = new RandomFreePort().value();
        this.server = new ArtipieServer(
            this.tmp, "my-npm", this.config(anonymous).toString(), this.port
        );
        this.server.start();
        this.url = String.format("http://host.testcontainers.internal:%d/my-npm/", this.port);
        Testcontainers.exposeHostPorts(this.port);
        this.cntn = new GenericContainer<>("node:14-alpine")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
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

    private String exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s", String.join(" ", command));
        return this.cntn.execInContainer(command).getStdout();
    }

    private RepoConfigYaml config(final boolean anonymous) {
        final RepoConfigYaml yaml = new RepoConfigYaml("npm")
            .withFileStorage(this.tmp.resolve("repos"))
            .withUrl(String.format("http://host.testcontainers.internal:%d/my-npm/", this.port));
        if (!anonymous) {
            yaml.withPermissions(
                new RepoPerms(ArtipieServer.ALICE.name(), "*")
            );
        }
        return yaml;
    }

    private boolean inNpmModule(final String proj, final String file) {
        return this.storage.exists(
            new Key.From("node_modules", proj, file)
        ).join();
    }

}

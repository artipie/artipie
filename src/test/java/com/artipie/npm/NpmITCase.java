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
import java.util.Optional;
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
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

/**
 * Integration tests for Npm repository.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.12
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
@EnabledOnOs({OS.LINUX, OS.MAC})
final class NpmITCase {

    /**
     * Project name.
     */
    private static final String PROJ = "@hello/simple-npm-project";

    /**
     * Host name.
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
     * Artipie server port.
     */
    private int port;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void npmInstall(final boolean anonymous) throws Exception {
        this.init(anonymous);
        this.saveFilesToStrg(NpmITCase.PROJ);
        MatcherAssert.assertThat(
            this.exec(
                "npm", "install", NpmITCase.PROJ,
                "--registry", this.url(this.userOpt(anonymous))
            ).getStdout(),
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
            this.exec(
                "npm", "publish", NpmITCase.PROJ,
                "--registry", this.url(this.userOpt(anonymous))
            ).getStdout(),
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
            this.exec(
                "npm", "install", NpmITCase.PROJ,
                "--registry", this.url(Optional.of(user))
            ).getStderr().replaceAll("\n", ""),
            new StringContains(
                String.format(
                    "npm ERR! 403 403 Forbidden - GET http://%s:***@%s:%d/my-npm/%s",
                    user.name(), NpmITCase.HOST, this.port, NpmITCase.PROJ.replace("/", "%2f")
                )
            )
        );
    }

    @Test
    void npmPublishFailWithForbidden() throws Exception {
        final ArtipieServer.User user = ArtipieServer.BOB;
        this.init(false);
        new TestResource("npm/simple-npm-project")
            .addFilesTo(this.storage, new Key.From(NpmITCase.PROJ));
        MatcherAssert.assertThat(
            "Package was published",
            this.exec(
                "npm", "publish", NpmITCase.PROJ,
                "--registry", this.url(Optional.of(user))
            ).getStderr().replaceAll("\n", ""),
            new StringContains(
                String.format(
                    "npm ERR! 403 403 Forbidden - PUT http://%s:***@%s:%d/my-npm/%s",
                    user.name(), NpmITCase.HOST, this.port, NpmITCase.PROJ.replace("/", "%2f")
                )
            )
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

    private Container.ExecResult exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s", String.join(" ", command));
        return this.cntn.execInContainer(command);
    }

    private RepoConfigYaml config(final boolean anonymous) {
        final RepoConfigYaml yaml = new RepoConfigYaml("npm")
            .withFileStorage(this.tmp.resolve("repos"))
            .withUrl(
                this.url(this.userOpt(anonymous))
            );
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

    private Optional<ArtipieServer.User> userOpt(final boolean anonymous) {
        final Optional<ArtipieServer.User> user;
        if (anonymous) {
            user = Optional.empty();
        } else {
            user = Optional.of(ArtipieServer.ALICE);
        }
        return user;
    }

    private String url(final Optional<ArtipieServer.User> usr) {
        String res = "";
        if (usr.isPresent()) {
            res = String.format(
                "%s:%s@", usr.get().name(), usr.get().password()
            );
        }
        return String.format(
            "http://%s%s:%d/my-npm/",
            res, NpmITCase.HOST, this.port
        );
    }

}

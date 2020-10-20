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
package com.artipie.file;

import com.artipie.ArtipieServer;
import com.artipie.RepoConfigYaml;
import com.artipie.RepoPermissions;
import com.artipie.RepoPerms;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.jcabi.log.Logger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.MatchesPattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

/**
 * Integration tests for Files repository.
 * @since 0.11
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings({
    "PMD.AvoidDuplicateLiterals",
    "PMD.TooManyMethods"
})
@EnabledOnOs({OS.LINUX, OS.MAC})
final class FilesRepoITCase {

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
    void curlGetShouldReceiveFile(final boolean anonymous) throws Exception {
        this.init(this.config(anonymous));
        new TestResource("file-repo")
            .addFilesTo(this.storage, new Key.From("repos", "my-file", "file-repo"));
        MatcherAssert.assertThat(
            this.curl("GET", this.userOpt(anonymous)),
            new MatchesPattern(
                Pattern.compile(
                    // @checkstyle LineLengthCheck (1 line)
                    "HTTP\\/1.1 200 OK[\\r\\n]{0,2}Content-Type: application\\/octet-stream[\\r\\n \\S]*"
                )
            )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void curlPutShouldSaveFile(final boolean anonymous) throws Exception {
        this.init(this.config(anonymous));
        MatcherAssert.assertThat(
            "curl PUT does work properly",
            this.curl("PUT", this.userOpt(anonymous)),
            new StringContains("HTTP/1.1 201 Created")
        );
        MatcherAssert.assertThat(
            "File should be saved in storage",
            this.storage.exists(
                new Key.From("repos", "my-file", "file-repo", "curl.txt")
            ).toCompletableFuture().join(),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "GET"})
    void curlPutAndGetShouldFailWithUnauthorized(final String req) throws Exception {
        this.init(this.config(false));
        MatcherAssert.assertThat(
            this.curl(
                req, Optional.of(
                    new ArtipieServer.User(
                        ArtipieServer.ALICE.name(),
                        String.format("bad%s", ArtipieServer.ALICE.password())
                    )
                )
            ),
            new StringContains("HTTP/1.1 401 Unauthorized")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT", "GET"})
    void curlPutAndGetShouldFailWithForbidden(final String req) throws Exception {
        this.init(this.config(false));
        MatcherAssert.assertThat(
            this.curl(req, Optional.of(ArtipieServer.BOB)),
            new StringContains("HTTP/1.1 403 Forbidden")
        );
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.cntn.stop();
    }

    private String curl(final String action,
        final Optional<ArtipieServer.User> user) throws Exception {
        final String url = "http://host.testcontainers.internal:%d/my-file/file-repo/curl.txt";
        final List<String> cmdlst = new ArrayList<>(
            Arrays.asList(
                "curl", "-i", "-X", action, String.format(url, this.port)
            )
        );
        user.ifPresent(
            usr -> {
                cmdlst.add("--user");
                cmdlst.add(String.format("%s:%s", usr.name(), usr.password()));
            }
        );
        final String[] cmdarr = cmdlst.toArray(new String[0]);
        Logger.debug(this, "Command:\n%s", String.join(" ", cmdlst));
        return this.cntn.execInContainer(cmdarr).getStdout();
    }

    private void init(final RepoConfigYaml config) throws Exception {
        this.storage = new FileStorage(this.tmp);
        this.server = new ArtipieServer(this.tmp, "my-file", config);
        this.port = this.server.start();
        this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        this.cntn = new GenericContainer<>("centos:centos8")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
        Logger.debug(this, "Command:\nyum -y install curl");
        this.cntn.execInContainer("yum", "-y", "install", "curl");
    }

    private RepoConfigYaml config(final boolean anonymous) {
        final RepoConfigYaml res = new RepoConfigYaml("file")
            .withFileStorage(this.tmp.resolve("repos"));
        if (!anonymous) {
            res.withPermissions(
                new RepoPerms(
                    new RepoPermissions.PermissionItem(
                        ArtipieServer.ALICE.name(),
                        new ListOf<String>("write", "download")
                    )
                )
            );
        }
        return res;
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

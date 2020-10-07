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
package com.artipie.rpm;

import com.artipie.ArtipieServer;
import com.artipie.RepoConfigYaml;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.rs.RsStatus;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
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
 * IT case for RPM repository.
 * @since 0.12
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
public final class RpmITCase {

    /**
     * Repo name.
     */
    private static final String REPO = "my-rpm";

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
     * Artipie server port.
     */
    private int port;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    @BeforeEach
    void init() throws IOException {
        this.server = new ArtipieServer(
            this.tmp, RpmITCase.REPO, new RepoConfigYaml("rpm").withFileStorage(this.tmp)
        );
        this.port = this.server.start();
    }

    @Test
    void addsRpmAndCreatesRepodata() throws Exception {
        final HttpURLConnection con = (HttpURLConnection) new URL(
            String.format(
                "http://localhost:%s/%s/time-1.7-45.el7.x86_64.rpm", this.port, RpmITCase.REPO
            )
        ).openConnection();
        con.setRequestMethod("PUT");
        con.setDoOutput(true);
        ByteStreams.copy(
            new ByteArrayInputStream(new TestResource("rpm/time-1.7-45.el7.x86_64.rpm").asBytes()),
            con.getOutputStream()
        );
        MatcherAssert.assertThat(
            "Response status is 202",
            con.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.ACCEPTED.code()))
        );
        MatcherAssert.assertThat(
            "Repository xml indexes are created",
            new FileStorage(this.tmp).list(new Key.From(RpmITCase.REPO, "repodata")).join().size(),
            new IsEqual<>(4)
        );
        con.disconnect();
    }

    @Test
    void listYumOperationWorks() throws Exception {
        this.prepareRpmRepository();
        this.prepareContainer();
        MatcherAssert.assertThat(
            "Lists 'time' package",
            this.yumExec("list"),
            new StringContainsInOrder(new ListOf<>("time.x86_64", "1.7-45.el7"))
        );
    }

    @Test
    void installYumOperationWorks() throws Exception {
        this.prepareRpmRepository();
        this.prepareContainer();
        MatcherAssert.assertThat(
            "Installs 'time' package",
            this.yumExec("install"),
            new StringContainsInOrder(new ListOf<>("time-1.7-45.el7.x86_64", "Complete!"))
        );
    }

    @AfterEach
    void close() {
        this.server.stop();
        if (this.cntn != null) {
            this.cntn.stop();
        }
    }

    private String yumExec(final String action) throws Exception {
        return this.cntn.execInContainer(
            "yum", "-y", "repo-pkgs", "example", action
        ).getStdout();
    }

    private void prepareContainer() throws IOException, InterruptedException {
        Testcontainers.exposeHostPorts(this.port);
        final Path setting = this.tmp.resolve("example.repo");
        this.tmp.resolve("example.repo").toFile().createNewFile();
        Files.write(
            setting,
            new ListOf<>(
                "[example]",
                "name=Example Repository",
                String.format(
                    "baseurl=http://host.testcontainers.internal:%d/%s", this.port, RpmITCase.REPO
                ),
                "enabled=1",
                "gpgcheck=0"
            )
        );
        this.cntn = new GenericContainer<>("centos:centos8")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
        this.cntn.execInContainer("mv", "/home/example.repo", "/etc/yum.repos.d/");
    }

    private void prepareRpmRepository() {
        final Storage storage = new FileStorage(this.tmp);
        new TestResource("rpm/time-1.7-45.el7.x86_64.rpm")
            .saveTo(storage, new Key.From(RpmITCase.REPO, "time-1.7-45.el7.x86_64.rpm"));
        new Rpm(
            storage,
            new RepoConfig.Simple(Digest.SHA256, new NamingPolicy.HashPrefixed(Digest.SHA1), true)
        ).batchUpdate(new Key.From(RpmITCase.REPO)).blockingAwait();
    }

}

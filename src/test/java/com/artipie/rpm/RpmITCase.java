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
import com.artipie.RepoPermissions;
import com.artipie.RepoPerms;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.rs.RsStatus;
import com.artipie.test.TestContainer;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.Testcontainers;

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
    private TestContainer cntn;

    void startArtipie(final boolean anonymous) throws IOException {
        final RepoConfigYaml config = new RepoConfigYaml("rpm").withFileStorage(this.tmp);
        if (!anonymous) {
            config.withPermissions(
                new RepoPerms(
                    new RepoPermissions.PermissionItem(
                        ArtipieServer.ALICE.name(), new ListOf<>("*")
                    )
                )
            );
        }
        this.server = new ArtipieServer(this.tmp, RpmITCase.REPO, config);
        this.port = this.server.start();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void addsRpmAndCreatesRepodata(final boolean anonymous) throws Exception {
        this.startArtipie(anonymous);
        final HttpURLConnection con = (HttpURLConnection) new URL(
            String.format(
                "http://localhost:%s/%s/time-1.7-45.el7.x86_64.rpm", this.port, RpmITCase.REPO
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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void listYumOperationWorks(final boolean anonymous) throws Exception {
        this.startArtipie(anonymous);
        this.prepareRpmRepository();
        this.prepareContainer(anonymous);
        MatcherAssert.assertThat(
            "Lists 'time' package",
            this.yumExec("list"),
            new StringContainsInOrder(new ListOf<>("time.x86_64", "1.7-45.el7"))
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void installYumOperationWorks(final boolean anonymous) throws Exception {
        this.startArtipie(anonymous);
        this.prepareRpmRepository();
        this.prepareContainer(anonymous);
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
            this.cntn.close();
        }
    }

    private String yumExec(final String action) throws Exception {
        return this.cntn.execStdout(
            "yum", "-y", "repo-pkgs", "example", action
        );
    }

    private void prepareContainer(final boolean anonymous)
        throws Exception {
        Testcontainers.exposeHostPorts(this.port);
        final Path setting = this.tmp.resolve("example.repo");
        this.tmp.resolve("example.repo").toFile().createNewFile();
        Files.write(
            setting,
            new ListOf<>(
                "[example]",
                "name=Example Repository",
                String.format(
                    "baseurl=http://%shost.testcontainers.internal:%d/%s", this.auth(anonymous),
                    this.port, RpmITCase.REPO
                ),
                "enabled=1",
                "gpgcheck=0"
            )
        );
        this.cntn = new TestContainer("centos:centos8", this.tmp, this.port);
        this.cntn.start();
        this.cntn.execStdout("mv", "/home/example.repo", "/etc/yum.repos.d/");
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

    private String auth(final boolean anonymous) {
        String res = "";
        if (!anonymous) {
            res = String.format(
                "%s:%s@", ArtipieServer.ALICE.name(), ArtipieServer.ALICE.password()
            );
        }
        return res;
    }

}

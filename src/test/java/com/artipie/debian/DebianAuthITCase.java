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
package com.artipie.debian;

import com.artipie.ArtipieServer;
import com.artipie.RepoConfigYaml;
import com.artipie.RepoPerms;
import com.artipie.asto.test.TestResource;
import com.artipie.http.rs.RsStatus;
import com.artipie.test.TestContainer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.codec.binary.Base64;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Debian integration test.
 * @since 0.15
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class DebianAuthITCase {

    /**
     * Repository name.
     */
    private static final String NAME = "my-debian";

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
     * Artipie server port.
     */
    private int port;

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void pushAndInstallWorks(final String method) throws Exception {
        this.init(new RepoPerms(ArtipieServer.ALICE.name(), "*"));
        final HttpURLConnection con = this.getConnection(ArtipieServer.ALICE.nameAndPswd(), method);
        final DataOutputStream out = new DataOutputStream(con.getOutputStream());
        out.write(new TestResource("debian/aglfn_1.7-3_amd64.deb").asBytes());
        out.close();
        MatcherAssert.assertThat(
            "Response for upload is OK",
            con.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.OK.code()))
        );
        this.cntn.execStdout("apt-get", "update");
        MatcherAssert.assertThat(
            "Package was downloaded and unpacked",
            this.cntn.execStdout("apt-get", "install", "-y", "aglfn"),
            new StringContainsInOrder(new ListOf<>("Unpacking aglfn", "Setting up aglfn"))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT"})
    void returnsUnauthorizedWhenUserIsUnknown(final String method) throws Exception {
        this.init(new RepoPerms(ArtipieServer.ALICE.name(), "*"));
        MatcherAssert.assertThat(
            "Response is UNAUTHORIZED",
            this.getConnection("mark:abc", method).getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.UNAUTHORIZED.code()))
        );
    }

    @ParameterizedTest
    @CsvSource({"GET,write", "PUT,read"})
    void returnsForbiddenWhenOperationIsNotAllowed(final String method, final String opr)
        throws Exception {
        this.init(new RepoPerms(ArtipieServer.ALICE.name(), opr));
        MatcherAssert.assertThat(
            "Response is FORBIDDEN",
            this.getConnection(ArtipieServer.ALICE.nameAndPswd(), method).getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.FORBIDDEN.code()))
        );
    }

    @AfterEach
    void stop() {
        this.server.stop();
        this.cntn.close();
    }

    private HttpURLConnection getConnection(final String auth, final String method)
        throws IOException {
        final HttpURLConnection con = (HttpURLConnection) new URL(
            String.format("http://localhost:%d/my-debian/main/aglfn_1.7-3_amd64.deb", this.port)
        ).openConnection();
        con.setDoOutput(true);
        con.addRequestProperty(
            "Authorization",
            String.format(
                "Basic %s",
                new String(
                    Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8))
                )
            )
        );
        con.setRequestMethod(method);
        return con;
    }

    private void init(final RepoPerms perms) throws Exception {
        this.server = new ArtipieServer(
            this.tmp, DebianAuthITCase.NAME,
            new RepoConfigYaml("deb").withFileStorage(this.tmp.resolve("repos"))
                .withPermissions(perms).withComponentsAndArchs("main", "amd64")
        );
        this.port = this.server.start();
        final Path setting = this.tmp.resolve("sources.list");
        Files.write(
            setting,
            String.format(
                "deb [trusted=yes] http://%s:%s@host.testcontainers.internal:%d/my-debian %s main",
                ArtipieServer.ALICE.name(), ArtipieServer.ALICE.password(),
                this.port, DebianAuthITCase.NAME
            ).getBytes()
        );
        this.cntn = new TestContainer("debian", this.tmp);
        this.cntn.start(this.port);
        this.cntn.execStdout("mv", "/home/sources.list", "/etc/apt/");
    }
}

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
package com.artipie.api;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.ArtipieServer;
import com.artipie.RepoConfigYaml;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import org.apache.commons.codec.binary.Base64;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * IT for Artipie API and dashboard.
 * @since 0.14
 */
class ArtipieApiITCase {

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
     * Port.
     */
    private int port;

    @BeforeEach
    void init() throws IOException {
        final Storage storage = new FileStorage(this.tmp);
        storage.save(
            new Key.From("repos/_permissions.yaml"), new Content.From(this.apiPerms().getBytes())
        ).join();
        this.server = new ArtipieServer(
            this.tmp, "my_repo",
            new RepoConfigYaml("binary")
                .withFileStorage(this.tmp.resolve("repos/test")),
            "org"
        );
        this.port = this.server.start();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "api/repos/bob",
            "dashboard/bob", "dashboard/bob/my_repo",
            "api/security/users/bob", "api/security/permissions/my_repo",
            "api/security/permissions"
        }
    )
    void getRequestsWork(final String url) throws Exception {
        final HttpURLConnection con = (HttpURLConnection)
            new URL(
                String.format("http://localhost:%s/%s", this.port, url)
            ).openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty(
            "Authorization",
            String.format(
                "Basic %s",
                new String(
                    Base64.encodeBase64(
                        String.format(
                            "%s:%s", ArtipieServer.BOB.name(), ArtipieServer.BOB.password()
                        ).getBytes()
                    )
                )
            )
        );
        MatcherAssert.assertThat(
            "Response status is 200",
            con.getResponseCode(),
            new IsEqual<>(HttpURLConnection.HTTP_OK)
        );
        con.disconnect();
    }

    @AfterEach
    void stop() {
        this.server.stop();
    }

    private String apiPerms() {
        return Yaml.createYamlMappingBuilder()
            .add(
                "permissions",
                Yaml.createYamlMappingBuilder()
                    .add(
                        ArtipieServer.BOB.name(),
                        Yaml.createYamlSequenceBuilder().add("api").build()
                    ).build()
            )
            .build().toString();
    }

}

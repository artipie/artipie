/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.ArtipieServer;
import com.artipie.RepoConfigYaml;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.repo.ConfigFile;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import org.apache.commons.codec.binary.Base64;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    /**
     * Http connection.
     */
    private HttpURLConnection con;

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
        this.init(ConfigFile.Extension.YAML.value());
        this.initConnection(url);
        MatcherAssert.assertThat(
            "Response status is 200",
            this.con.getResponseCode(),
            new IsEqual<>(HttpURLConnection.HTTP_OK)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "api/repos/bob,.yaml", "api/repos/bob,.yml",
        "dashboard/bob,.yaml", "dashboard/bob,.yml",
        "api/security/users/bob,.yaml", "api/security/users/bob,.yml"
    })
    void readsConfigWithYamlAndYmlExtension(final String url, final String extension)
        throws Exception {
        this.init(extension);
        this.initConnection(url);
        MatcherAssert.assertThat(
            "Response status is 200 for different extension",
            this.con.getResponseCode(),
            new IsEqual<>(HttpURLConnection.HTTP_OK)
        );
    }

    @AfterEach
    void stop() {
        this.con.disconnect();
        this.server.stop();
    }

    private void initConnection(final String url) throws Exception {
        this.con = (HttpURLConnection)
            new URL(
                String.format("http://localhost:%s/%s", this.port, url)
            ).openConnection();
        this.con.setRequestMethod("GET");
        this.con.setRequestProperty(
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
    }

    private void init(final String extension) throws IOException {
        final Storage storage = new FileStorage(this.tmp.resolve("repos"));
        storage.save(
            new Key.From(String.format("_permissions%s", extension)),
            new Content.From(this.apiPerms().getBytes())
        ).join();
        this.server = new ArtipieServer(
            this.tmp, "my_repo",
            new RepoConfigYaml("binary")
                .withFileStorage(this.tmp.resolve("repos/test")),
            "org"
        );
        this.port = this.server.start();
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

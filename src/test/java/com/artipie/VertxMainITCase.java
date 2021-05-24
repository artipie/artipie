/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.http.rs.RsStatus;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link VertxMain}.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class VertxMainITCase {

    /**
     * Repository name.
     */
    private static final String REPO_NAME = "my-file";

    /**
     * Temporary directory for all tests.
     *
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    /**
     * Artipie server.
     */
    private ArtipieServer server;

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Repo path.
     */
    private Path path;

    @BeforeEach
    void init() {
        this.path = this.tmp.resolve("repos");
        this.storage = new FileStorage(this.path);
        this.storage.save(
            new Key.From(VertxMainITCase.REPO_NAME, "item.txt"), Content.EMPTY
        ).join();
    }

    @Test
    void startsWhenNotValidRepoConfigsArePresent() throws IOException {
        this.storage.save(
            new Key.From("invalid_repo.yaml"), new Content.From("any text.yaml".getBytes())
        ).join();
        this.server = new ArtipieServer(
            this.tmp, VertxMainITCase.REPO_NAME,
            new RepoConfigYaml("file").withFileStorage(this.path),
            Optional.empty()
        );
        final int port = this.server.start();
        final HttpURLConnection con = (HttpURLConnection) new URL(this.formatUrl(port))
            .openConnection();
        con.setRequestMethod("GET");
        MatcherAssert.assertThat(
            "Artipie is started and responding 200 ",
            con.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.OK.code()))
        );
        con.disconnect();
    }

    @Test
    void worksWhenRepoConfigsKeyIsPresent() throws IOException {
        this.server = new ArtipieServer(
            this.tmp, VertxMainITCase.REPO_NAME,
            new RepoConfigYaml("file").withFileStorage(this.path),
            Optional.of(new Key.From("my_configs"))
        );
        final int port = this.server.start();
        final HttpURLConnection con = (HttpURLConnection) new URL(this.formatUrl(port))
            .openConnection();
        con.setRequestMethod("GET");
        MatcherAssert.assertThat(
            "Artipie is started and responding 200 ",
            con.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.OK.code()))
        );
        con.disconnect();
    }

    @AfterEach
    void stop() {
        this.server.stop();
    }

    private String formatUrl(final int port) {
        return String.format("http://localhost:%s/%s/item.txt", port, VertxMainITCase.REPO_NAME);
    }

}

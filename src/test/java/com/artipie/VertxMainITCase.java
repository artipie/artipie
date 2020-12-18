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

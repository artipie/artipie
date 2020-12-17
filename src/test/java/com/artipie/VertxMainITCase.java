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
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link VertxMain}.
 * @since 0.1
 */
class VertxMainITCase {

    /**
     * Temporary directory for all tests.
     *
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    @Test
    void startsWhenNotValidRepoConfigsArePresent() throws IOException {
        final Path repos = this.tmp.resolve("repos");
        final Storage storage = new FileStorage(repos);
        storage.save(
            new Key.From("invalid_repo.yaml"), new Content.From("any text.yaml".getBytes())
        ).join();
        storage.save(new Key.From("my-file/item.txt"), Content.EMPTY).join();
        final ArtipieServer server = new ArtipieServer(
            this.tmp, "my-file", new RepoConfigYaml("file").withFileStorage(repos)
        );
        final int port = server.start();
        final HttpURLConnection con = (HttpURLConnection) new URL(
            String.format("http://localhost:%s/my-file/item.txt", port)
        ).openConnection();
        con.setRequestMethod("GET");
        MatcherAssert.assertThat(
            "Artipie is started and responding 200 ",
            con.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.OK.code()))
        );
        con.disconnect();
        server.stop();
    }

}

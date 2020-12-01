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
package com.artipie.gem;

import com.artipie.ArtipieServer;
import com.artipie.RepoConfigYaml;
import com.artipie.RepoPerms;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.test.RepositoryUrl;
import com.artipie.test.TestContainer;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import org.cactoos.text.Base64Encoded;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Integration tests for Gem repository.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.13
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class GemITCase {

    /**
     * Repo.
     */
    private static final String REPO = "my-gem";

    /**
     * Rails gem.
     */
    private static final String RAILS = "rails-6.0.2.2.gem";

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
     * Repository url.
     */
    private RepositoryUrl url;

    /**
     * Storage.
     */
    private Storage storage;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void gemPushWorks(final boolean anonymous) throws Exception {
        this.init(anonymous);
        this.push(anonymous);
        MatcherAssert.assertThat(
            this.storage.exists(
                new Key.From("repos", GemITCase.REPO, "gems", GemITCase.RAILS)
            ).toCompletableFuture().join(),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void gemInstallPushedGemWorks(final boolean anonymous) throws Exception {
        this.init(anonymous);
        this.push(anonymous);
        this.cntn.execStdout("gem", "sources", "--remove", "https://rubygems.org/");
        MatcherAssert.assertThat(
            this.cntn.execStdout(
                "gem", "install", GemITCase.RAILS,
                "--source", this.url.string(anonymous),
                "--ignore-dependencies", "-V"
            ),
            new AllOf<>(
                Arrays.asList(
                    new StringContains("Successfully installed rails-6.0.2.21"),
                    new StringContains(
                        String.format(
                            "GET %squick/Marshal.4.8/%sspec.rz200 OK",
                            this.url.string(anonymous), GemITCase.RAILS
                        )
                    )
                )
            )
        );
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.cntn.close();
    }

    private void push(final boolean anonymous) throws Exception {
        new TestResource(String.format("gem/%s", GemITCase.RAILS))
            .saveTo(this.storage, new Key.From(GemITCase.RAILS));
        final String tmpurl = this.url.string(true);
        final String apikey;
        if (anonymous) {
            apikey = new Base64Encoded("any:any").asString();
        } else {
            apikey = new Base64Encoded(
                String.format("%s:%s", ArtipieServer.ALICE.name(), ArtipieServer.ALICE.password())
            ).asString();
        }
        this.cntn.execStdout(
            "/bin/bash", "-c",
            String.format(
                "GEM_HOST_API_KEY=%s gem push %s --host %s",
                apikey,
                GemITCase.RAILS,
                tmpurl.substring(0, tmpurl.length() - 1)
            )
        );
    }

    private void init(final boolean anonymous) throws IOException {
        this.server = new ArtipieServer(
            this.tmp, GemITCase.REPO, this.config(anonymous).toString()
        );
        this.storage = new FileStorage(this.tmp);
        final int port = this.server.start();
        this.url = new RepositoryUrl(port, GemITCase.REPO);
        this.cntn = new TestContainer("ruby:2.7.2", this.tmp);
        this.cntn.start(port);
    }

    private RepoConfigYaml config(final boolean anonymous) {
        final RepoConfigYaml yaml = new RepoConfigYaml("gem")
            .withFileStorage(this.tmp.resolve("repos"));
        if (!anonymous) {
            yaml.withPermissions(
                new RepoPerms(ArtipieServer.ALICE.name(), "*")
            );
        }
        return yaml;
    }

}

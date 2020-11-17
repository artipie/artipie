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
import org.cactoos.text.Base64Encoded;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for Helm repository.
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

    @Test
    public void gemPushWorks() throws Exception {
        final boolean anonymous = true;
        this.init(anonymous);
        this.push(anonymous);
        MatcherAssert.assertThat(
            this.storage.exists(
                new Key.From("repos", GemITCase.REPO, "gems", GemITCase.RAILS)
            ).toCompletableFuture().join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void gemInstallPushedGemWorks() throws Exception {
        final boolean anonymous = true;
        this.init(anonymous);
        this.push(anonymous);
        MatcherAssert.assertThat(
            this.cntn.execStdout("gem", "install", GemITCase.RAILS, "--ignore-dependencies"),
            new StringContains("Successfully installed rails-6.0.2.21")
        );
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.cntn.close();
    }

    private void push(final boolean anonymous) throws Exception {
        final String key = new Base64Encoded("usr:pwd").asString();
        new TestResource(String.format("gem/%s", GemITCase.RAILS))
            .saveTo(this.storage, new Key.From(GemITCase.RAILS));
        final String urlwithslash = this.url.string(anonymous);
        this.cntn.execStdout(
            "/bin/bash", "-c",
            String.format(
                "GEM_HOST_API_KEY=%s gem push %s --host %s",
                key, GemITCase.RAILS,
                urlwithslash.substring(0, urlwithslash.length() - 1)
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

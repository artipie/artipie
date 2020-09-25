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

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.jcabi.log.Logger;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.MatchesPattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

/**
 * Integration tests for Files repository.
 * @since 0.11
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class FilesRepoITCase {

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
    private GenericContainer<?> cntn;

    /**
     * Storage.
     */
    private Storage storage;

    /**
     * Artipie server port.
     */
    private int port;

    @BeforeEach
    void init() throws Exception {
        this.storage = new FileStorage(this.tmp);
        this.server = new ArtipieServer(this.tmp, "my-file", this.config());
        this.port = this.server.start();
        this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        this.cntn = new GenericContainer<>("centos:centos8")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
        this.exec("yum", "-y", "install", "curl");
    }

    @Test
    void curlGetShouldReceiveFile() throws Exception {
        final String url = "http://host.testcontainers.internal:%d/my-file/file-repo/curl.txt";
        this.addFilesToStorage(
            "file-repo", new Key.From("repos", "my-file", "file-repo")
        );
        MatcherAssert.assertThat(
            this.exec("curl", "-i", "-X", "GET", String.format(url, this.port)),
            new MatchesPattern(
                Pattern.compile(
                    // @checkstyle LineLengthCheck (1 line)
                    "HTTP\\/1.1 200 OK[\\r\\n]{0,2}Content-Type: application\\/octet-stream[\\r\\n \\S]*"
                )
            )
        );
    }

    @Test
    void curlPutShouldSaveFile() throws Exception {
        final String url = "http://host.testcontainers.internal:%d/my-file/file-repo/curl.txt";
        MatcherAssert.assertThat(
            "curl PUT does work properly",
            this.exec("curl", "-i", "-X", "PUT", String.format(url, this.port)),
            new StringContains("HTTP/1.1 201 Created")
        );
        MatcherAssert.assertThat(
            "File should be saved in storage",
            this.storage.exists(
                new Key.From("repos", "my-file", "file-repo", "curl.txt")
            ).toCompletableFuture().join(),
            new IsEqual<>(true)
        );
    }

    @AfterEach
    void release() throws Exception {
        this.server.stop();
        this.cntn.stop();
    }

    private String exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s", String.join(" ", command));
        return this.cntn.execInContainer(command).getStdout();
    }

    private String config() {
        return Yaml.createYamlMappingBuilder().add(
            "repo",
            Yaml.createYamlMappingBuilder()
                .add("type", "file")
                .add(
                    "storage",
                    Yaml.createYamlMappingBuilder()
                        .add("type", "fs")
                        .add("path", this.tmp.resolve("repos").toString())
                        .build()
                )
                .build()
        ).build().toString();
    }

    private void addFilesToStorage(final String resource, final Key key)
        throws InterruptedException {
        final Storage resources = new FileStorage(
            new TestResource(resource).asPath()
        );
        final BlockingStorage bsto = new BlockingStorage(resources);
        for (final Key item : bsto.list(Key.ROOT)) {
            new BlockingStorage(this.storage).save(
                new Key.From(key, item),
                bsto.value(new Key.From(item))
            );
        }
    }
}

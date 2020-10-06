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
package com.artipie.pypi;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.ArtipieServer;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

/**
 * Integration tests for Pypi repository.
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class PypiITCase {

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
    void init() throws IOException, InterruptedException {
        this.storage = new FileStorage(this.tmp);
        this.server = new ArtipieServer(this.tmp, "my-pypi", this.config());
        this.port = this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        this.cntn = new GenericContainer<>("python:3")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
    }

    @Test
    void pypiInstall() throws Exception {
        final String url = String.format(
            "http://host.testcontainers.internal:%d/my-pypi/", this.port
        );
        this.addPackageToStorage();
        MatcherAssert.assertThat(
            this.exec(
                "pip", "install", "--verbose", "--index-url",  url, "--no-deps",
                "--trusted-host", "host.testcontainers.internal", "alarmtime==0.1.5"
            ),
            Matchers.containsString("Successfully installed alarmtime-0.1.5")
        );
    }

    @AfterEach
    void release() {
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
                .add("type", "pypi")
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

    private void addPackageToStorage() {
        new TestResource("pypi-repo/alarmtime-0.1.5.tar.gz")
            .saveTo(
                this.storage,
                new Key.From("repos", "my-pypi", "alarmtime", "alarmtime-0.1.5.tar.gz")
        );
    }
}

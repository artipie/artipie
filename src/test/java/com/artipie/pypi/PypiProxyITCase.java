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
import com.amihaiemil.eoyaml.YamlMappingBuilder;
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
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test to pypi proxy.
 * @since 0.12
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
@Disabled
public final class PypiProxyITCase {

    /**
     * Host.
     */
    private static final String HOST = "host.testcontainers.internal";

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    /**
     * Test origin.
     */
    private ArtipieServer origin;

    /**
     * Storage.
     */
    private Storage storage;

    /**
     * Test proxy.
     */
    private ArtipieServer proxy;

    /**
     * Container.
     */
    private TestContainer cntn;

    /**
     * Artipie proxy server port.
     */
    private int port;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void installFromProxy(final boolean anonymous) throws Exception {
        this.init(anonymous);
        new TestResource("pypi-repo/alarmtime-0.1.5.tar.gz").saveTo(
            this.storage,
            new Key.From("origin", "my-pypi", "alarmtime", "alarmtime-0.1.5.tar.gz")
        );
        MatcherAssert.assertThat(
            this.cntn.execStdout(
                "pip", "install", "--no-deps", "--trusted-host", PypiProxyITCase.HOST,
                "--index-url", new RepositoryUrl(this.port, "my-pypi-proxy").string(anonymous),
                "alarmtime"
            ),
            Matchers.containsString("Successfully installed alarmtime-0.1.5")
        );
    }

    @AfterEach
    void stop() {
        this.proxy.stop();
        this.origin.stop();
        this.cntn.close();
    }

    private void init(final boolean anonymous) throws IOException {
        this.storage = new FileStorage(this.tmp);
        this.origin = new ArtipieServer(
            this.tmp, "my-pypi", this.originConfig(anonymous)
        );
        this.proxy = new ArtipieServer(
            this.tmp, "my-pypi-proxy", this.proxyConfig(anonymous, this.origin.start())
        );
        this.port = this.proxy.start();
        this.cntn = new TestContainer("python:3", this.tmp);
        this.cntn.start(this.port);
    }

    private RepoConfigYaml originConfig(final boolean anonymous) {
        final RepoConfigYaml yaml = new RepoConfigYaml("pypi")
            .withFileStorage(this.tmp.resolve("origin"));
        if (!anonymous) {
            yaml.withPermissions(
                new RepoPerms(ArtipieServer.ALICE.name(), "*")
            );
        }
        return yaml;
    }

    private RepoConfigYaml proxyConfig(final boolean anonymous, final int prt) {
        final RepoConfigYaml yaml = new RepoConfigYaml("pypi-proxy");
        final String url = String.format("http://localhost:%d/my-pypi", prt);
        final YamlMappingBuilder rmts = Yaml.createYamlMappingBuilder()
            .add("url", url)
            .add(
                "cache",
                Yaml.createYamlMappingBuilder().add(
                    "storage",
                    Yaml.createYamlMappingBuilder()
                        .add("type", "fs")
                        .add("path", this.tmp.resolve("proxy").toString())
                        .build()
                ).build()
            );
        if (anonymous) {
            yaml.withRemotes(Yaml.createYamlSequenceBuilder().add(rmts.build()));
        } else {
            yaml.withRemotes(
                Yaml.createYamlSequenceBuilder().add(
                    rmts.add("username", ArtipieServer.ALICE.name())
                        .add("password", ArtipieServer.ALICE.password())
                        .build()
                )
            );
        }
        return yaml;
    }

}

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
package com.artipie.maven;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.ArtipieServer;
import com.artipie.RepoConfigYaml;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.test.TestContainer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for {@link com.artipie.maven.http.MavenProxySlice}.
 *
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.11
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class MavenProxyIT {

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    /**
     * Local Artipie server that stores artifacts.
     */
    private ArtipieServer server;

    /**
     * Container for local server.
     */
    private TestContainer cntn;

    /**
     * Storage.
     */
    private Storage storage;

    /**
     * Proxy Artipie server port.
     */
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        this.storage = new FileStorage(this.tmp);
        this.server = new ArtipieServer(
            this.tmp, "my-maven",
            new RepoConfigYaml("maven-proxy").withRemotes(
                Yaml.createYamlSequenceBuilder()
                    .add(
                        Yaml.createYamlMappingBuilder()
                            .add("url", "https://repo.maven.apache.org/maven2")
                            .add(
                                "cache",
                                Yaml.createYamlMappingBuilder().add(
                                    "storage",
                                    Yaml.createYamlMappingBuilder()
                                        .add("type", "fs")
                                        .add("path", this.tmp.resolve("repos").toString())
                                        .build()
                                ).build()
                            )
                            .build()
                    )
            )
        );
        this.port = this.server.start();
        final Path setting = this.tmp.resolve("settings.xml");
        setting.toFile().createNewFile();
        Files.write(setting, this.settings());
        this.cntn = new TestContainer("centos:centos8", this.tmp, this.port);
        this.cntn.start();
        this.cntn.execStdout("yum", "-y", "install", "maven");
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.cntn.close();
    }

    @Test
    void shouldGetArtifactFromCentralAndSaveInCache() throws Exception {
        final String artifact = "-Dartifact=args4j:args4j:2.32:jar";
        MatcherAssert.assertThat(
            "Artifact wasn't downloaded",
            this.cntn.execStdout(
                "mvn", "-s", "/home/settings.xml", "dependency:get", artifact
            ),
            new StringContains("BUILD SUCCESS")
        );
        MatcherAssert.assertThat(
            "Artifact wasn't saved in cache",
            this.storage.exists(new Key.From("repos/my-maven/args4j/args4j/2.32/args4j-2.32.jar"))
                .toCompletableFuture().join(),
            new IsEqual<>(true)
        );
    }

    private List<String> settings() {
        return new ListOf<String>(
            "<settings>",
            "    <profiles>",
            "        <profile>",
            "            <id>artipie</id>",
            "            <repositories>",
            "                <repository>",
            "                    <id>my-maven</id>",
            String.format("<url>http://host.testcontainers.internal:%d/my-maven/</url>", this.port),
            "                </repository>",
            "            </repositories>",
            "        </profile>",
            "    </profiles>",
            "    <activeProfiles>",
            "        <activeProfile>artipie</activeProfile>",
            "    </activeProfiles>",
            "</settings>"
        );
    }
}

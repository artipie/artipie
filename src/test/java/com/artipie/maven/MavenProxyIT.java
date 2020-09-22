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
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.jcabi.log.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

/**
 * Integration test for {@link com.artipie.maven.http.MavenProxySlice}.
 *
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.11
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs(OS.LINUX)
@Disabled
final class MavenProxyIT {

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    /**
     * Subdirectory in temporary directory.
     */
    private Path subdir;

    /**
     * Local Artipie server that stores artifacts.
     */
    private ArtipieServer server;

    /**
     * Container for local server.
     */
    private GenericContainer<?> cntn;

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
        this.subdir = Files.createDirectory(Path.of(this.tmp.toString(), "subdir"));
        this.storage = new FileStorage(this.subdir);
        this.server = new ArtipieServer(this.subdir, "my-maven", this.configsProxy());
        this.port = this.server.start();
        final Path setting = this.subdir.resolve("settings.xml");
        setting.toFile().createNewFile();
        Files.write(setting, this.settings());
        this.cntn = new GenericContainer<>("centos:centos8")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.subdir.toString(), "/home");
        Testcontainers.exposeHostPorts(this.port);
        this.cntn.start();
        this.cntn.execInContainer("yum", "-y", "install", "maven");
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.cntn.close();
    }

    @Test
    void shouldGetArtifactFromLocalAndSaveInCache() throws Exception {
        final String artifact = "-Dartifact=aspectj:aspectj-ant:1.0.6:jar";
        this.exec("mvn", "-s", "/home/settings.xml", "dependency:get", artifact);
        MatcherAssert.assertThat(
            "Artifact wasn't downloaded",
            this.exec(
                "mvn", "-s", "/home/settings.xml", "dependency:get", artifact
            ).replaceAll("\n", ""),
            new AllOf<>(
                Arrays.asList(
                    new StringContains("BUILD SUCCESS"),
                    new IsNot<>(new StringContains("Downloaded"))
                )
            )
        );
        MatcherAssert.assertThat(
            "Artifact wasn't saved in cache",
            this.storage.exists(new Key.From("args4j", "args4j", "2.32", "args4j-2.32"))
                .toCompletableFuture().join(),
            new IsEqual<>(true)
        );
    }

    private String exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s", String.join(" ", command));
        return this.cntn.execInContainer(command).getStdout();
    }

    private String configsProxy() {
        return Yaml.createYamlMappingBuilder().add(
            "repo",
            Yaml.createYamlMappingBuilder()
                .add("type", "maven-proxy")
                .add(
                    "storage",
                    Yaml.createYamlMappingBuilder()
                        .add("type", "fs")
                        .add("path", this.subdir.resolve("repos").toString())
                        .build()
                )
                .add(
                    "settings",
                    Yaml.createYamlMappingBuilder()
                        .add("remote_uri", "https://repo.maven.apache.org/")
                        .build()
                ).build()
        ).build().toString();
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

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
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.cactoos.list.ListOf;
import org.cactoos.scalar.Unchecked;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.MatchesPattern;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

/**
 * Integration tests for Maven repository.
 * @since 0.11
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class MavenITCase {

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
        this.tmp.toFile().setWritable(true);
        this.subdir = Files.createDirectory(Path.of(this.tmp.toString(), "subdir"));
        this.storage = new FileStorage(this.subdir);
        this.server = new ArtipieServer(this.subdir, "my-maven", this.configs());
        this.port = this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        final Path setting = this.subdir.resolve("settings.xml");
        setting.toFile().createNewFile();
        Files.write(setting, this.settings());
        this.cntn = new GenericContainer<>("centos:centos8")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.subdir.toString(), "/home");
        this.cntn.start();
        this.cntn.execInContainer("yum", "-y", "install", "maven");
    }

    @ParameterizedTest
    @CsvSource({"helloworld,0.1", "snapshot,1.0-SNAPSHOT"})
    void downloadsArtifact(final String type, final String vers) throws Exception {
        this.addFilesToStorage(
            String.format("com/artipie/%s", type),
            new Key.From("repos", "my-maven", "com", "artipie", type)
        );
        MatcherAssert.assertThat(
            this.exec(
                "mvn", "-s", "/home/settings.xml", "dependency:get",
                String.format("-Dartifact=com.artipie:%s:%s", type, vers)
            ).replaceAll("\n", ""),
            new StringContainsInOrder(
                new ListOf<String>(
                    // @checkstyle LineLengthCheck (2 lines)
                    String.format(
                        "Downloaded from my-maven: http://host.testcontainers.internal:%d/my-maven/com/artipie/%s/%s/%s-%s.jar",
                        this.port, type, vers, type, vers
                    ),
                    "BUILD SUCCESS"
                )
            )
        );
    }

    @ParameterizedTest
    @CsvSource({"helloworld,0.1,0.1", "snapshot,1.0-SNAPSHOT,1.0-[\\d-.]{17}"})
    void deploysArtifact(final String type, final String vers, final String assembly)
        throws Exception {
        this.prepareDirectory(
            String.format("%s-src", type),
            String.format("%s-src/pom.xml", type)
        );
        MatcherAssert.assertThat(
            "Build failure",
            this.exec(
                "mvn", "-s", "/home/settings.xml", "-f",
                String.format("/home/%s-src/pom.xml", type),
                "deploy"
            ).replaceAll("\n", ""),
            new StringContains("BUILD SUCCESS")
        );
        MatcherAssert.assertThat(
            "Artifacts weren't added to storage",
            this.storage.list(
                new Key.From("repos", "my-maven", "com", "artipie", type)
            ).join().stream()
            .map(Key::string)
            .collect(Collectors.toList())
            .toString()
            .replaceAll("\n", ""),
            new AllOf<>(
                Arrays.asList(
                    new MatchesPattern(
                        Pattern.compile(
                            String.format(
                                ".*repos/my-maven/com/artipie/%s/maven-metadata.xml.*", type
                            )
                        )
                    ),
                    new MatchesPattern(
                        Pattern.compile(
                            String.format(
                                ".*repos/my-maven/com/artipie/%s/%s/%s-%s.pom.*",
                                type, vers, type, assembly
                            )
                        )
                    ),
                    new MatchesPattern(
                        Pattern.compile(
                            String.format(
                                ".*repos/my-maven/com/artipie/%s/%s/%s-%s.jar.*",
                                type, vers, type, assembly
                            )
                        )
                    )
                )
            )
        );
    }

    @AfterEach
    void stopContainer() {
        this.server.stop();
        this.cntn.stop();
    }

    private void prepareDirectory(final String src, final String pom) throws IOException {
        FileUtils.copyDirectory(
            new TestResource(src).asPath().toFile(),
            this.subdir.resolve(src).toFile()
        );
        Files.write(
            this.subdir.resolve(pom),
            String.format(
                Files.readString(this.subdir.resolve(pom)),
                this.port
            ).getBytes()
        );
    }

    /**
     * Executes dnf command in container.
     * @param command What to do
     * @return String stdout
     * @throws Exception On error
     */
    private String exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s", String.join(" ", command));
        return this.cntn.execInContainer(command).getStdout();
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

    private String configs() {
        return Yaml.createYamlMappingBuilder().add(
            "repo",
            Yaml.createYamlMappingBuilder()
                .add("type", "maven")
                .add(
                    "storage",
                    Yaml.createYamlMappingBuilder()
                        .add("type", "fs")
                        .add("path", this.subdir.resolve("repos").toString())
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

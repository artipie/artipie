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

import com.artipie.ArtipieServer;
import com.artipie.RepoConfigYaml;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.test.TestContainer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.cactoos.list.ListOf;
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
     * Tested Artipie server.
     */
    private ArtipieServer server;

    /**
     * Container.
     */
    private TestContainer cntn;

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
        this.server = new ArtipieServer(
            this.tmp, "my-maven",
            new RepoConfigYaml("maven").withFileStorage(this.tmp.resolve("repos"))
        );
        this.port = this.server.start();
        final Path setting = this.tmp.resolve("settings.xml");
        setting.toFile().createNewFile();
        Files.write(setting, this.settings());
        this.cntn = new TestContainer("centos:centos8", this.tmp, this.port);
        this.cntn.start();
        this.cntn.execStdout("yum", "-y", "install", "maven");
    }

    @ParameterizedTest
    @CsvSource({"helloworld,0.1", "snapshot,1.0-SNAPSHOT"})
    void downloadsArtifact(final String type, final String vers) throws Exception {
        new TestResource(String.format("com/artipie/%s", type))
            .addFilesTo(this.storage, new Key.From("repos", "my-maven", "com", "artipie", type));
        MatcherAssert.assertThat(
            this.cntn.execStdout(
                "mvn", "-s", "/home/settings.xml", "dependency:get",
                String.format("-Dartifact=com.artipie:%s:%s", type, vers)
            ),
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
            this.cntn.execStdout(
                "mvn", "-s", "/home/settings.xml", "-f",
                String.format("/home/%s-src/pom.xml", type),
                "deploy"
            ),
            new StringContains("BUILD SUCCESS")
        );
        this.cntn.execStdout(
            "mvn", "-s", "/home/settings.xml", "-f",
            String.format("/home/%s-src/pom.xml", type),
            "clean"
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
    void release() {
        this.server.stop();
        this.cntn.close();
    }

    private void prepareDirectory(final String src, final String pom) throws IOException {
        FileUtils.copyDirectory(
            new TestResource(src).asPath().toFile(),
            this.tmp.resolve(src).toFile()
        );
        Files.write(
            this.tmp.resolve(pom),
            String.format(
                Files.readString(this.tmp.resolve(pom)),
                this.port
            ).getBytes()
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

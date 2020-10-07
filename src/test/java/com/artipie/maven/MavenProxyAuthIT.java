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
import com.artipie.RepoPermissions;
import com.artipie.RepoPerms;
import com.artipie.asto.Key;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
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
@DisabledOnOs(OS.WINDOWS)
final class MavenProxyAuthIT {

    /**
     * Temporary directory for all tests.
     *
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    /**
     * Artipie origin.
     */
    private ArtipieServer origin;

    /**
     * Artipie proxy.
     */
    private ArtipieServer proxy;

    /**
     * Container for local server.
     */
    private GenericContainer<?> cntn;

    @BeforeEach
    void setUp() throws Exception {
        this.startOrigin();
        this.startProxy();
        final Path setting = this.tmp.resolve("settings.xml");
        setting.toFile().createNewFile();
        Files.write(setting, this.settings());
        this.cntn = new GenericContainer<>("centos:centos8")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        Testcontainers.exposeHostPorts(this.proxy.port());
        this.cntn.start();
        this.exec("yum", "-y", "install", "maven");
    }

    @AfterEach
    void tearDown() {
        this.cntn.close();
        this.proxy.stop();
        this.origin.stop();
    }

    @Test
    @Disabled
    void shouldGetDependency() throws Exception {
        MatcherAssert.assertThat(
            this.exec(
                "mvn",
                "-s", "/home/settings.xml",
                "dependency:get", "-Dartifact=com.artipie:helloworld:0.1:jar"
            ).replaceAll("\n", ""),
            new StringContains("BUILD SUCCESS")
        );
    }

    private String exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s", String.join(" ", command));
        return this.cntn.execInContainer(command).getStdout();
    }

    private void startOrigin() throws IOException {
        final Path root = this.tmp.resolve("origin");
        root.toFile().mkdirs();
        final Path repos = root.resolve("repos");
        new TestResource("com/artipie/helloworld").addFilesTo(
            new FileStorage(repos),
            new Key.From("maven-origin/com/artipie/helloworld")
        );
        this.origin = new ArtipieServer(
            root,
            "maven-origin",
            new RepoConfigYaml("maven").withFileStorage(repos).withPermissions(
                new RepoPerms(
                    new RepoPermissions.PermissionItem(
                        ArtipieServer.ALICE.name(),
                        Collections.singletonList("r")
                    )
                )
            )
        );
        this.origin.start();
    }

    private void startProxy() throws IOException {
        final Path root = this.tmp.resolve("proxy");
        root.toFile().mkdirs();
        this.proxy = new ArtipieServer(
            root,
            "my-maven",
            new RepoConfigYaml("maven-proxy")
                .withFileStorage(root.resolve("repos"))
                .withRemoteUri(
                    String.format("http://localhost:%s/maven-origin", this.origin.port()),
                    ArtipieServer.ALICE.name(),
                    ArtipieServer.ALICE.password()
                )
        );
        this.proxy.start();
    }

    private List<String> settings() {
        return new ListOf<>(
            "<settings>",
            "    <profiles>",
            "        <profile>",
            "            <id>artipie</id>",
            "            <repositories>",
            "                <repository>",
            "                    <id>my-maven</id>",
            String.format(
                "<url>http://host.testcontainers.internal:%d/my-maven/</url>",
                this.proxy.port()
            ),
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

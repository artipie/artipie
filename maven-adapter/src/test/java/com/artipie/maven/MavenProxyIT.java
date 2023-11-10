/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.maven;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.cache.FromStorageCache;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.maven.http.MavenProxySlice;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
@EnabledOnOs({OS.LINUX, OS.MAC})
final class MavenProxyIT {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Storage.
     */
    private Storage storage;

    /**
     * Vertx slice server port.
     */
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        final JettyClientSlices slices = new JettyClientSlices();
        slices.start();
        this.storage = new InMemoryStorage();
        this.server = new VertxSliceServer(
            MavenProxyIT.VERTX,
            new LoggingSlice(
                new MavenProxySlice(
                    slices,
                    URI.create("https://repo.maven.apache.org/maven2"),
                    Authenticator.ANONYMOUS,
                    new FromStorageCache(this.storage)
            ))
        );
        this.port = this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        this.cntn = new GenericContainer<>("maven:3.6.3-jdk-11")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
    }

    @AfterEach
    void tearDown() {
        this.server.close();
        this.cntn.stop();
    }

    @AfterAll
    static void close() {
        MavenProxyIT.VERTX.close();
    }

    @Test
    void shouldGetArtifactFromCentralAndSaveInCache() throws Exception {
        this.settings();
        final String artifact = "-Dartifact=args4j:args4j:2.32:jar";
        MatcherAssert.assertThat(
            "Artifact wasn't downloaded",
            this.exec(
                "mvn", "-s", "/home/settings.xml", "dependency:get", artifact
            ).replaceAll("\n", ""),
            new AllOf<>(
                Arrays.asList(
                    new StringContains("BUILD SUCCESS"),
                    new StringContains(
                        String.format(
                            // @checkstyle LineLengthCheck (1 line)
                            "Downloaded from my-repo: http://host.testcontainers.internal:%s/args4j/args4j/2.32/args4j-2.32.jar (154 kB",
                            this.port
                        )
                    )
                )
            )
        );
        MatcherAssert.assertThat(
            "Artifact wasn't in storage",
            this.storage.exists(new Key.From("args4j", "args4j", "2.32", "args4j-2.32.jar"))
                .toCompletableFuture().join(),
            new IsEqual<>(true)
        );
    }

    private String exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s", String.join(" ", command));
        return this.cntn.execInContainer(command).getStdout();
    }

    private void settings() throws IOException {
        final Path setting = this.tmp.resolve("settings.xml");
        setting.toFile().createNewFile();
        Files.write(
            setting,
            new ListOf<String>(
                "<settings>",
                "    <profiles>",
                "        <profile>",
                "            <id>artipie</id>",
                "            <repositories>",
                "                <repository>",
                "                    <id>my-repo</id>",
                String.format("<url>http://host.testcontainers.internal:%d/</url>", this.port),
                "                </repository>",
                "            </repositories>",
                "        </profile>",
                "    </profiles>",
                "    <activeProfiles>",
                "        <activeProfile>artipie</activeProfile>",
                "    </activeProfiles>",
                "</settings>"
            )
        );
    }
}

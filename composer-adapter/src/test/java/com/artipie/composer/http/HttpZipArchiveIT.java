/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http;

import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.composer.AstoRepository;
import com.artipie.composer.test.ComposerSimple;
import com.artipie.composer.test.HttpUrlUpload;
import com.artipie.http.misc.RandomFreePort;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.policy.Policy;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

/**
 * Integration test for PHP Composer repository for working
 * with archive in ZIP format.
 */
@DisabledOnOs(OS.WINDOWS)
final class HttpZipArchiveIT {
    /**
     * Vertx instance for using in test.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Temporary directory.
     */
    private Path tmp;

    /**
     * HTTP server hosting repository.
     */
    private VertxSliceServer server;

    /**
     * Test container.
     */
    private GenericContainer<?> cntn;

    /**
     * Server url.
     */
    private String url;

    /**
     * Server port.
     */
    private int port;

    /**
     * Artifact events.
     */
    private Queue<ArtifactEvent> events;

    @BeforeEach
    void setUp() throws IOException {
        this.tmp = Files.createTempDirectory("");
        this.port = RandomFreePort.get();
        this.url = String.format("http://host.testcontainers.internal:%s", this.port);
        this.events = new LinkedList<>();
        final AstoRepository asto = new AstoRepository(
            new FileStorage(this.tmp), Optional.of(this.url)
        );
        this.server = new VertxSliceServer(
            HttpZipArchiveIT.VERTX,
            new LoggingSlice(
                new PhpComposer(
                    asto,  Policy.FREE, (username, password) -> Optional.empty(),
                    "*", Optional.of(this.events)
                )
            ),
            this.port
        );
        this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        this.cntn = new GenericContainer<>("composer:2.0.9")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.cntn.stop();
        try {
            FileUtils.cleanDirectory(this.tmp.toFile());
            Files.deleteIfExists(this.tmp);
        } catch (final IOException ex) {
            Logger.error(this, "Failed to clean directory %[exception]s", ex);
        }
    }

    @AfterAll
    static void close() {
        HttpZipArchiveIT.VERTX.close();
    }

    @Test
    void shouldInstallAddedPackageThroughComposerRepo() throws Exception {
        this.addArchive();
        MatcherAssert.assertThat("One event was added to queue", this.events.size() == 1);
        new ComposerSimple(this.url, "psr/log", "1.1.3")
            .writeTo(this.tmp.resolve("composer.json"));
        MatcherAssert.assertThat(
            this.exec("composer", "install", "--verbose", "--no-cache"),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    new StringContains(false, "Installs: psr/log:1.1.3"),
                    new StringContains(false, "- Downloading psr/log (1.1.3)"),
                    new StringContains(
                        false,
                        "- Installing psr/log (1.1.3): Extracting archive"
                    )
                )
            )
        );
    }

    @Test
    void shouldInstallAddedPackageThroughArtifactsRepo() throws Exception {
        this.addArchive();
        MatcherAssert.assertThat("One event was added to queue", this.events.size() == 1);
        this.writeComposer("artifacts");
        MatcherAssert.assertThat(
            this.exec("composer", "install", "--verbose", "--no-cache"),
            new AllOf<>(
                new ListOf<>(
                    new StringContains(false, "Installs: psr/log:1.1.3"),
                    new StringContains(false, "- Downloading psr/log (1.1.3)"),
                    new StringContains(
                        false,
                        "- Installing psr/log (1.1.3): Extracting archive"
                    )
                )
            )
        );
    }

    @Test
    void shouldFailGetAbsentInArtifactsPackage() throws Exception {
        this.tmp.resolve("artifacts").toFile().mkdir();
        this.writeComposer("artifacts");
        MatcherAssert.assertThat(
            this.exec("composer", "install", "--verbose", "--no-cache"),
            new StringContains(
                "Root composer.json requires psr/log, it could not be found in any version"
            )
        );
    }

    @Test
    void shouldFailGetPackageInCaseOfWrongUrl() throws Exception {
        final String wrong = "wrongfolder";
        this.tmp.resolve(wrong).toFile().mkdir();
        this.addArchive();
        this.writeComposer(wrong);
        MatcherAssert.assertThat(
            this.exec("composer", "install", "--verbose", "--no-cache"),
            new StringContains(
                "Root composer.json requires psr/log, it could not be found in any version"
            )
        );
    }

    private void addArchive() throws Exception {
        final String name = "log-1.1.3.zip";
        new HttpUrlUpload(
            String.format("http://localhost:%d/%s", this.port, name),
            new TestResource(name).asBytes()
        ).upload(Optional.empty());
    }

    private void writeComposer(final String path) throws IOException {
        Files.write(
            this.tmp.resolve("composer.json"),
            String.join(
                "",
                "{",
                "\"repositories\": [",
                String.format("{\"type\": \"artifact\", \"url\": \"%s\"},", path),
                "{\"packagist.org\": false}",
                "],",
                "\"require\": { \"psr/log\": \"1.1.3\" }",
                "}"
            ).getBytes()
        );
    }

    private String exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s\n", String.join(" ", command));
        final Container.ExecResult res = this.cntn.execInContainer(command);
        final String log = String.format(
            "STDOUT:\n%s\nSTDERR:\n%s", res.getStdout(), res.getStderr()
        );
        Logger.debug(this, log);
        return log;
    }
}

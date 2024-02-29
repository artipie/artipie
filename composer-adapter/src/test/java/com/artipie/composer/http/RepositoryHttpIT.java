/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http;

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.composer.AstoRepository;
import com.artipie.composer.test.ComposerSimple;
import com.artipie.composer.test.HttpUrlUpload;
import com.artipie.composer.test.PackageSimple;
import com.artipie.composer.test.SourceServer;
import com.artipie.http.misc.RandomFreePort;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.security.policy.Policy;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import org.cactoos.list.ListOf;
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
import java.util.Optional;

/**
 * Integration test for PHP Composer repository.
 */
@DisabledOnOs(OS.WINDOWS)
class RepositoryHttpIT {
    /**
     * Vertx instance for using in test.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Temporary directory.
     */
    private Path temp;

    /**
     * Path to PHP project directory.
     */
    private Path project;

    /**
     * HTTP server hosting repository.
     */
    private VertxSliceServer server;

    /**
     * HTTP source server.
     */
    private SourceServer sourceserver;

    /**
     * Repository URL.
     */
    private String url;

    /**
     * Test container.
     */
    private GenericContainer<?> cntn;

    /**
     * Server port.
     */
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        this.temp = Files.createTempDirectory("");
        this.project = this.temp.resolve("project");
        this.project.toFile().mkdirs();
        this.server = new VertxSliceServer(
            RepositoryHttpIT.VERTX,
            new LoggingSlice(
                new PhpComposer(
                    new AstoRepository(new InMemoryStorage()),
                    Policy.FREE,
                    (username, password) -> Optional.empty(),
                    "*", Optional.empty()
                )
            )
        );
        this.port = this.server.start();
        final int sourceport = new RandomFreePort().get();
        this.sourceserver = new SourceServer(RepositoryHttpIT.VERTX, sourceport);
        Testcontainers.exposeHostPorts(this.port, sourceport);
        this.cntn = new GenericContainer<>("composer:2.0.9")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.project.toString(), "/home");
        this.cntn.start();
        this.url = String.format("http://host.testcontainers.internal:%s", this.port);
    }

    @AfterEach
    void tearDown() {
        if (this.sourceserver != null) {
            this.sourceserver.close();
        }
        this.server.stop();
        this.cntn.stop();
        try {
            FileUtils.cleanDirectory(this.temp.toFile());
            Files.deleteIfExists(this.temp);
        } catch (final IOException ex) {
            Logger.error(this, "Failed to clean directory %[exception]s", ex);
        }
    }

    @AfterAll
    static void close() {
        RepositoryHttpIT.VERTX.close();
    }

    @Test
    void shouldInstallAddedPackageWithVersion() throws Exception {
        this.addPackage();
        new ComposerSimple(this.url).writeTo(this.project.resolve("composer.json"));
        MatcherAssert.assertThat(
            this.exec("composer", "install", "--verbose", "--no-cache"),
            new AllOf<>(
                new ListOf<>(
                    new StringContains(false, "Installs: vendor/package:1.1.2"),
                    new StringContains(false, "- Downloading vendor/package (1.1.2)"),
                    new StringContains(
                        false,
                        "- Installing vendor/package (1.1.2): Extracting archive"
                    )
                )
            )
        );
    }

    @Test
    void shouldInstallAddedPackageWithoutVersion() throws Exception {
        new HttpUrlUpload(
            String.format("http://localhost:%s/?version=2.3.4", this.port),
            new PackageSimple(this.sourceserver.upload()).withoutVersion()
        ).upload(Optional.empty());
        new ComposerSimple(this.url, "vendor/package", "2.3.4")
            .writeTo(this.project.resolve("composer.json"));
        MatcherAssert.assertThat(
            this.exec("composer", "install", "--verbose", "--no-cache"),
            new AllOf<>(
                new ListOf<>(
                    new StringContains(false, "Installs: vendor/package:2.3.4"),
                    new StringContains(false, "- Downloading vendor/package (2.3.4)"),
                    new StringContains(
                        false,
                        "- Installing vendor/package (2.3.4): Extracting archive"
                    )
                )
            )
        );
    }

    private void addPackage() throws Exception {
        new HttpUrlUpload(
            String.format("http://localhost:%s", this.port),
            new PackageSimple(this.sourceserver.upload()).withSetVersion()
        ).upload(Optional.empty());
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

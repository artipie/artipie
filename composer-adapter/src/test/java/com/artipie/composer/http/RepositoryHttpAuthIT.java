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
import com.artipie.composer.test.TestAuthentication;
import com.artipie.http.Slice;
import com.artipie.http.misc.RandomFreePort;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.security.policy.PolicyByUsername;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
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

/**
 * Integration test for PHP Composer repository with auth.
 */
@DisabledOnOs(OS.WINDOWS)
final class RepositoryHttpAuthIT {
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
        this.port = RandomFreePort.get();
        final int sourceport = RandomFreePort.get();
        final Slice slice = new PhpComposer(
            new AstoRepository(new InMemoryStorage()),
            new PolicyByUsername(TestAuthentication.ALICE.name()),
            new TestAuthentication(),
            "test", Optional.empty()
        );
        this.server = new VertxSliceServer(
            RepositoryHttpAuthIT.VERTX,
            new LoggingSlice(slice),
            this.port
        );
        this.server.start();
        this.sourceserver = new SourceServer(RepositoryHttpAuthIT.VERTX, sourceport);
        Testcontainers.exposeHostPorts(this.port, sourceport);
        this.cntn = new GenericContainer<>("composer:2.0.9")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.project.toString(), "/home");
        this.cntn.start();
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
        RepositoryHttpAuthIT.VERTX.close();
    }

    @Test
    void shouldInstallAddedPackageWithAuth() throws Exception {
        this.addPackage();
        new ComposerSimple(this.url(TestAuthentication.ALICE))
            .writeTo(this.project.resolve("composer.json"));
        MatcherAssert.assertThat(
            this.exec("composer", "install", "--verbose", "--no-cache"),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
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
    void returnsUnauthorizedWhenUserIsUnknown() throws Exception {
        this.addPackage();
        new ComposerSimple(this.url(TestAuthentication.BOB))
            .writeTo(this.project.resolve("composer.json"));
        MatcherAssert.assertThat(
            this.exec("composer", "install", "--verbose", "--no-cache"),
            new StringContains("URL required authentication")
        );
    }

    private void addPackage() throws Exception {
        new HttpUrlUpload(
            String.format("http://localhost:%s", this.port),
            new PackageSimple(this.sourceserver.upload()).withSetVersion()
        ).upload(Optional.of(TestAuthentication.ALICE));
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

    private String url(final TestAuthentication.User user) {
        return String.format(
            "http://%s:%s@host.testcontainers.internal:%d",
            user.name(),
            user.password(),
            this.port
        );
    }
}

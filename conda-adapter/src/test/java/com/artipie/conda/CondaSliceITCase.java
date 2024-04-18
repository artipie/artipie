/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.conda.http.CondaSlice;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.misc.RandomFreePort;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.policy.Policy;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import org.apache.commons.io.FileUtils;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Conda adapter integration test.
 */
@DisabledOnOs(OS.WINDOWS)
public final class CondaSliceITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Temp directory.
     */
    private Path tmp;

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Application port.
     */
    private int port;

    /**
     * Artifact events.
     */
    private Queue<ArtifactEvent> events;

    @BeforeEach
    void initialize() throws Exception {
        this.tmp = Files.createTempDirectory("conda-test");
        this.storage = new InMemoryStorage();
        this.port = RandomFreePort.get();
        this.events = new ConcurrentLinkedDeque<>();
        final String url = String.format("http://host.testcontainers.internal:%d", this.port);
        this.server = new VertxSliceServer(
            CondaSliceITCase.VERTX,
            new LoggingSlice(
                new BodyLoggingSlice(
                    new CondaSlice(
                        storage,
                        Policy.FREE,
                        (username, password) -> Optional.of(AuthUser.ANONYMOUS),
                        new TestCondaTokens(),
                        url, "*",
                        Optional.of(events)
                    )
                )
            ),
            this.port
        );
        this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        Files.write(
            this.tmp.resolve(".condarc"), String.format("channels:\n  - %s", url).getBytes()
        );
        this.cntn = new GenericContainer<>("artipie/conda-tests:1.0")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/w/adapter/example-project")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
    }

    @Test
    @Disabled("https://github.com/artipie/artipie/issues/1336")
    void anacondaCanLogin() throws Exception {
        this.exec(
            "anaconda", "config", "--set", "url",
            String.format("http://host.testcontainers.internal:%d/", this.port), "-s"
        );
        MatcherAssert.assertThat(
            this.exec("anaconda", "-v", "login", "--username", "any", "--password", "any"),
            new StringContainsInOrder(
                new ListOf<>(
                    "Using Anaconda API: http://host.testcontainers.internal",
                    "any's login successful"
                )
            )
        );
        MatcherAssert.assertThat(
            this.exec("anaconda", "logout"),
            new StringContainsInOrder(
                new ListOf<>(
                    "Using Anaconda API: http://host.testcontainers.internal", "logout successful"
                )
            )
        );
        MatcherAssert.assertThat(
            this.exec("anaconda", "-v", "login", "--username", "alice", "--password", "abc123"),
            new StringContainsInOrder(
                new ListOf<>(
                    "Using Anaconda API: http://host.testcontainers.internal",
                    "alice's login successful"
                )
            )
        );
    }

    @Test
    void canPublishTwoVersionsWithCondaBuildAndThenInstall() throws Exception {
        this.moveCondarc();
        this.exec(
            "anaconda", "config", "--set", "url",
            String.format("http://host.testcontainers.internal:%d/", this.port),
            "-s"
        );
        this.exec("conda", "config", "--set", "anaconda_upload", "yes");
        MatcherAssert.assertThat(
            "Login was not successful",
            this.exec("anaconda", "login", "--username", "anonymous", "--password", "any"),
            new StringContains("anonymous's login successful")
        );
        this.uploadAndCheck("0.0.1");
        this.uploadAndCheck("0.0.2");
        MatcherAssert.assertThat(
            "Example-package 0.0.2 was not installed",
            exec("conda", "install", "--verbose", "-y", "example-package"),
            new StringContainsInOrder(
                new ListOf<String>(
                    "The following packages will be downloaded:",
                    "http://host.testcontainers.internal",
                    "linux-64::example-package-0.0.2-0",
                    "Executing transaction: ...working... done"
                )
            )
        );
        MatcherAssert.assertThat(
            "Packages info was added to events queue", this.events.size() == 2
        );
    }

    @Test
    void canInstallWithCondaInstall() throws Exception {
        this.moveCondarc();
        new TestResource("CondaSliceITCase/packages.json")
            .saveTo(this.storage, new Key.From("linux-64/repodata.json"));
        new TestResource("CondaSliceITCase/snappy-1.1.3-0.tar.bz2")
            .saveTo(this.storage, new Key.From("linux-64/snappy-1.1.3-0.tar.bz2"));
        MatcherAssert.assertThat(
            exec("conda", "install", "--verbose", "-y", "snappy"),
            new StringContainsInOrder(
                new ListOf<String>(
                    "The following packages will be downloaded:",
                    "http://host.testcontainers.internal",
                    "linux-64::snappy-1.1.3-0",
                    "Preparing transaction: ...working... done",
                    "Verifying transaction: ...working... done",
                    "Executing transaction: ...working... done"
                )
            )
        );
    }

    @AfterEach
    void stop() {
        this.server.stop();
        this.cntn.stop();
        if (this.tmp != null) {
            FileUtils.deleteQuietly(this.tmp.toFile());
        }
    }

    private String exec(final String... command) throws Exception {
        final Container.ExecResult res = this.cntn.execInContainer(command);
        Logger.debug(this, "Command:\n%s\nResult:\n%s", String.join(" ", command), res.toString());
        return res.toString();
    }

    private void moveCondarc() throws Exception {
        this.cntn.execInContainer("mv", "/home/.condarc", "/root/");
    }

    private void uploadAndCheck(final String version) throws Exception {
        MatcherAssert.assertThat(
            "Package was not installed successfully",
            this.exec(
                "conda", "build", "--output-folder",
                String.format("./%s/conda-out/", version), String.format("./%s/conda/", version)
            ),
            new StringContainsInOrder(
                new ListOf<String>(
                    "Creating package \"example-package\"",
                    String.format("Creating release \"%s\"", version),
                    String.format("Uploading file \"anonymous/example-package/%s/linux-64/example-package-%s-0.tar.bz2\"", version, version),
                    "Upload complete"
                )
            )
        );
        MatcherAssert.assertThat(
            "Package not found in storage",
            this.storage.exists(
                new Key.From(String.format("linux-64/example-package-%s-0.tar.bz2", version))
            ).join(),
            new IsEqual<>(true)
        );
    }
}

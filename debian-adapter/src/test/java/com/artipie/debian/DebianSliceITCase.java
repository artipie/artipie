/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.debian.http.DebianSlice;
import com.artipie.http.RsStatus;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.policy.Policy;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.MatchesPattern;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

/**
 * Test for {@link com.artipie.debian.http.DebianSlice}.
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
public final class DebianSliceITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Temporary directory for all tests.
     */
    @TempDir
    Path tmp;

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Artipie port.
     */
    private int port;

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Artifact events queue.
     */
    private Queue<ArtifactEvent> events;

    @BeforeEach
    void init() throws IOException, InterruptedException {
        this.storage = new InMemoryStorage();
        this.events = new ConcurrentLinkedQueue<>();
        this.server = new VertxSliceServer(
            DebianSliceITCase.VERTX,
            new LoggingSlice(
                new DebianSlice(
                    this.storage,
                    Policy.FREE,
                    (username, password) -> Optional.empty(),
                    new Config.FromYaml(
                        "artipie",
                        Yaml.createYamlMappingBuilder()
                            .add("Components", "main")
                            .add("Architectures", "amd64")
                            .build(),
                        new InMemoryStorage()
                    ),
                    Optional.ofNullable(this.events)
                )
            )
        );
        this.port = this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        final Path setting = this.tmp.resolve("sources.list");
        Files.write(
            setting,
            String.format(
                "deb [trusted=yes] http://host.testcontainers.internal:%d/ artipie main", this.port
            ).getBytes()
        );
        this.cntn = new GenericContainer<>("artipie/deb-tests:1.0")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
        this.cntn.execInContainer("mv", "/home/sources.list", "/etc/apt/");
        this.cntn.execInContainer("ls", "-la", "/etc/apt/");
        this.cntn.execInContainer("cat", "/etc/apt/sources.list");
    }

    @Test
    void searchWorks() throws Exception {
        this.copyPackage("pspp_1.2.0-3_amd64.deb");
        this.cntn.execInContainer("apt-get", "update");
        MatcherAssert.assertThat(
            this.exec("apt-cache", "search", "pspp"),
            new StringContainsInOrder(new ListOf<>("pspp", "Statistical analysis tool"))
        );
    }

    @Test
    void installWorks() throws Exception {
        this.copyPackage("aglfn_1.7-3_amd64.deb");
        this.cntn.execInContainer("apt-get", "update");
        MatcherAssert.assertThat(
            "Package was downloaded and unpacked",
            this.exec("apt-get", "install", "-y", "aglfn"),
            new StringContainsInOrder(new ListOf<>("Unpacking aglfn", "Setting up aglfn"))
        );
    }

    @Test
    void installWithInReleaseFileWorks() throws Exception {
        this.copyPackage("aglfn_1.7-3_amd64.deb");
        MatcherAssert.assertThat(
            "Release file is used on update the world",
            this.exec("apt-get", "update"),
            Matchers.allOf(
                new MatchesPattern(Pattern.compile("[\\S\\s]*Get:2 http://host.testcontainers.internal:\\d+ artipie Release[\\S\\s]*")),
                new MatchesPattern(Pattern.compile("[\\S\\s]*Get:4 http://host.testcontainers.internal:\\d+ artipie/main amd64 Packages \\[1351 B][\\S\\s]*")),
                new IsNot<>(new StringContains("Get:5"))
            )
        );
        MatcherAssert.assertThat(
            "Package was downloaded and unpacked",
            this.exec("apt-get", "install", "-y", "aglfn"),
            Matchers.allOf(
                new MatchesPattern(Pattern.compile("[\\S\\s]*Get:1 http://host.testcontainers.internal:\\d+ artipie/main amd64 aglfn amd64 1.7-3 \\[29.9 kB][\\S\\s]*")),
                new IsNot<>(new StringContains("Get:2")),
                new StringContainsInOrder(new ListOf<>("Unpacking aglfn", "Setting up aglfn"))
            )
        );
    }

    @Test
    void pushAndInstallWorks() throws Exception {
        final HttpURLConnection con = (HttpURLConnection) URI.create(
            String.format("http://localhost:%d/main/aglfn_1.7-3_amd64.deb", this.port)
        ).toURL().openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("PUT");
        final DataOutputStream out = new DataOutputStream(con.getOutputStream());
        out.write(new TestResource("aglfn_1.7-3_amd64.deb").asBytes());
        out.close();
        MatcherAssert.assertThat(
            "Response for upload is OK",
            con.getResponseCode(),
            new IsEqual<>(RsStatus.OK.code())
        );
        MatcherAssert.assertThat("Event was added to queue", this.events.size() == 1);
        this.exec("apt-get", "update");
        MatcherAssert.assertThat(
            "Package was downloaded and unpacked",
            this.exec("apt-get", "install", "-y", "aglfn"),
            new StringContainsInOrder(new ListOf<>("Unpacking aglfn", "Setting up aglfn"))
        );
        MatcherAssert.assertThat("Pushed artifact added to events queue", this.events.size() == 1);
    }

    @AfterEach
    void stop() {
        this.server.stop();
        this.cntn.stop();
    }

    private void copyPackage(final String pkg) {
        new TestResource(pkg).saveTo(this.storage, new Key.From("main", pkg));
        new TestResource("Packages.gz")
            .saveTo(this.storage, new Key.From("dists/artipie/main/binary-amd64/Packages.gz"));
    }

    private String exec(final String... command) throws Exception {
        final Container.ExecResult res = this.cntn.execInContainer(command);
        Logger.debug(this, "Command:\n%s\nResult:\n%s", String.join(" ", command), res.toString());
        return res.getStdout();
    }
}

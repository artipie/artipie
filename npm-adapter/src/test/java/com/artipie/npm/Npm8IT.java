/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.npm.http.NpmSlice;
import com.artipie.npm.misc.JsonFromPublisher;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import javax.json.JsonObject;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

/**
 * Make sure the library is compatible with npm 8 cli tools.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
public final class Npm8IT {

    /**
     * Temporary directory for all tests.
     */
    private Path tmp;

    /**
     * Vert.x used to create tested FileStorage.
     */
    private Vertx vertx;

    /**
     * Storage used as client-side data (for packages to publish and npm-client settings).
     */
    private Storage data;

    /**
     * Storage used for repository data.
     */
    private Storage repo;

    /**
     * Server.
     */
    private VertxSliceServer server;

    /**
     * Repository URL.
     */
    private String url;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Test artifact events.
     */
    private Queue<ArtifactEvent> events;

    @BeforeEach
    void setUp() throws Exception {
        this.tmp = Files.createTempDirectory("npm-test");
        this.vertx = Vertx.vertx();
        this.data = new FileStorage(this.tmp);
        this.repo = new InMemoryStorage();
        final int port = new RandomFreePort().value();
        this.url = String.format("http://host.testcontainers.internal:%d", port);
        this.events = new LinkedList<>();
        this.server = new VertxSliceServer(
            this.vertx,
            new LoggingSlice(new NpmSlice(URI.create(this.url).toURL(), this.repo, this.events)),
            port
        );
        this.server.start();
        Testcontainers.exposeHostPorts(port);
        this.cntn = new GenericContainer<>("node:18-alpine")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
        this.data.save(
            new Key.From(".npmrc"),
            new Content.From(
                String.format("//host.testcontainers.internal:%d/:_authToken=abc123", port)
                    .getBytes(StandardCharsets.UTF_8)
            )
        ).join();
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.vertx.close();
        this.cntn.stop();
        FileUtils.deleteQuietly(this.tmp.toFile());
    }

    @ParameterizedTest
    @CsvSource({
        "@hello/simple-npm-project,simple-npm-project",
        "simple-npm-project,project-without-scope",
        "@scope.dot_01/project-scope-with-dot,project-scope-with-dot"
    })
    void npmPublishWorks(final String proj, final String resource) throws Exception {
        new TestResource(resource).addFilesTo(
            this.data,
            new Key.From(String.format("tmp/%s", proj))
        );
        this.exec(
            "npm", "publish", String.format("tmp/%s/", proj), "--registry", this.url,
            "--loglevel", "verbose"
        );
        final JsonObject meta = new JsonFromPublisher(
            this.repo.value(new Key.From(String.format("%s/meta.json", proj)))
                .toCompletableFuture().join()
        ).json().toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Metadata should be valid",
            meta.getJsonObject("versions")
                .getJsonObject("1.0.1")
                .getJsonObject("dist")
                .getString("tarball"),
            new IsEqual<>(String.format("/%s/-/%s-1.0.1.tgz", proj, proj))
        );
        MatcherAssert.assertThat(
            "File should be in storage after publishing",
            this.repo.exists(
                new Key.From(String.format("%s/-/%s-1.0.1.tgz", proj, proj))
            ).toCompletableFuture().join(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat("Events queue has one item", this.events.size() == 1);
    }

    @Test
    void npmInstallWorks() throws Exception {
        final String proj = "@hello/simple-npm-project";
        this.saveFilesToRegistry(proj);
        MatcherAssert.assertThat(
            this.exec("npm", "install", proj, "--registry", this.url, "--loglevel", "verbose"),
            new StringContainsInOrder(Arrays.asList("added 1 package", this.url, proj))
        );
        MatcherAssert.assertThat(
            "Installed project should contain index.js",
            this.inNpmModule(proj, "index.js"),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Installed project should contain package.json",
            this.inNpmModule(proj, "package.json"),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "@hello/simple-npm-project,simple-npm-project",
        "simple-npm-project,project-without-scope",
        "@scope.dot_01/project-scope-with-dot,project-scope-with-dot"
    })
    void installsPublishedProject(final String proj, final String resource) throws Exception {
        new TestResource(resource).addFilesTo(
            this.data,
            new Key.From(String.format("tmp/%s", proj))
        );
        this.exec("npm", "publish", String.format("tmp/%s/", proj), "--registry", this.url);
        MatcherAssert.assertThat(
            this.exec("npm", "install", proj, "--registry", this.url, "--loglevel", "verbose"),
            new StringContainsInOrder(Arrays.asList("added 1 package", proj, this.url))
        );
        MatcherAssert.assertThat("Events queue has one item", this.events.size() == 1);
    }

    private void saveFilesToRegistry(final String proj) {
        new TestResource(String.format("storage/%s/meta.json", proj)).saveTo(
            this.repo,
            new Key.From(proj, "meta.json")
        );
        new TestResource(String.format("storage/%s/-/%s-1.0.1.tgz", proj, proj)).saveTo(
            this.repo,
            new Key.From(proj, "-", String.format("%s-1.0.1.tgz", proj))
        );
    }

    private boolean inNpmModule(final String proj, final String file) {
        return this.data.exists(new Key.From("node_modules", proj, file)).join();
    }

    private String exec(final String... command) throws Exception {
        final Container.ExecResult res = this.cntn.execInContainer(command);
        Logger.debug(this, "Command:\n%s\nResult:\n%s", String.join(" ", command), res.toString());
        return res.toString();
    }
}

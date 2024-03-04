/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.npm.http.NpmSlice;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import javax.json.JsonObject;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Make sure the library is compatible with npm cli tools.
 */
@DisabledOnOs(OS.WINDOWS)
public final class NpmIT {

    /**
     * Vert.x used to create tested FileStorage.
     */
    private Vertx vertx;

    /**
     * Storage used as client-side data (for packages to publish).
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

    @BeforeEach
    void setUp(final @TempDir Path dtmp, final @TempDir Path rtmp) throws Exception {
        this.vertx = Vertx.vertx();
        this.data = new FileStorage(dtmp);
        this.repo = new FileStorage(rtmp);
        final int port = new RandomFreePort().value();
        this.url = String.format("http://host.testcontainers.internal:%d", port);
        this.server = new VertxSliceServer(
            this.vertx,
            new NpmSlice(URI.create(this.url).toURL(), this.repo),
            port
        );
        this.server.start();
        Testcontainers.exposeHostPorts(port);
        this.cntn = new GenericContainer<>("node:14-alpine")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(dtmp.toString(), "/home");
        this.cntn.start();
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.vertx.close();
        this.cntn.stop();
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
        this.exec("npm", "publish", String.format("tmp/%s", proj), "--registry", this.url);
        final JsonObject meta = this.repo.value(
            new Key.From(String.format("%s/meta.json", proj))
        ).join().asJsonObject();
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
    }

    @Test
    void npmInstallWorks() throws Exception {
        final String proj = "@hello/simple-npm-project";
        this.saveFilesToRegustry();
        MatcherAssert.assertThat(
            this.exec("npm", "install", proj, "--registry", this.url),
            new StringContainsInOrder(
                Arrays.asList(String.format("+ %s@1.0.1", proj), "added 1 package")
            )
        );
        MatcherAssert.assertThat(
            "Installed project should contain index.js",
            this.inNpmModule("index.js"),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Installed project should contain package.json",
            this.inNpmModule("package.json"),
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
            this.data, new Key.From(String.format("tmp/%s", proj))
        );
        this.exec("npm", "publish", String.format("tmp/%s", proj), "--registry", this.url);
        MatcherAssert.assertThat(
            this.exec("npm", "install", proj, "--registry", this.url),
            new StringContainsInOrder(
                Arrays.asList(
                    String.format("+ %s@1.0.1", proj),
                    "added 1 package"
                )
            )
        );
    }

    private void saveFilesToRegustry() {
        new TestResource(String.format("storage/%s/meta.json", "@hello/simple-npm-project")).saveTo(
            this.repo, new Key.From("@hello/simple-npm-project", "meta.json")
        );
        new TestResource(String.format("storage/%s/-/%s-1.0.1.tgz", "@hello/simple-npm-project", "@hello/simple-npm-project")).saveTo(
            this.repo, new Key.From("@hello/simple-npm-project", "-", String.format("%s-1.0.1.tgz", "@hello/simple-npm-project"))
        );
    }

    private boolean inNpmModule(final String file) {
        return this.data.exists(new Key.From("node_modules", "@hello/simple-npm-project", file)).join();
    }

    private String exec(final String... command) throws Exception {
        final Container.ExecResult res = this.cntn.execInContainer(command);
        Logger.debug(this, "Command:\n%s\nResult:\n%s", String.join(" ", command), res.toString());
        return res.getStdout();
    }
}

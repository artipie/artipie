/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
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
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

/**
 * IT for `npm unpublish` command.
 * @since 0.8
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@DisabledOnOs(OS.WINDOWS)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class NpmUnpublishIT {

    /**
     * Vert.x used to create tested FileStorage.
     */
    private Vertx vertx;

    /**
     * Storage used as repository.
     */
    private Storage storage;

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
    void setUp(final @TempDir Path tmp) throws Exception {
        this.vertx = Vertx.vertx();
        this.storage = new InMemoryStorage();
        final int port = new RandomFreePort().value();
        this.url = String.format("http://host.testcontainers.internal:%d", port);
        this.server = new VertxSliceServer(
            this.vertx,
            new LoggingSlice(new NpmSlice(URI.create(this.url).toURL(), this.storage)),
            port
        );
        this.server.start();
        Testcontainers.exposeHostPorts(port);
        this.cntn = new GenericContainer<>("node:14-alpine")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(tmp.toString(), "/home");
        this.cntn.start();
        final Storage data = new FileStorage(tmp);
        data.save(
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
    }

    @Test
    void npmUnpublishWholePackageWorks() throws Exception {
        final String proj = "@hello/simple-npm-project";
        new TestResource("storage").addFilesTo(this.storage, Key.ROOT);
        MatcherAssert.assertThat(
            "Unpublish command is succeeded",
            this.exec("npm", "unpublish", proj, "--force", "--registry", this.url),
            new StringContains(String.format("- %s", proj))
        );
        MatcherAssert.assertThat(
            "The entire package was removed",
            this.storage.list(new Key.From(proj)).join().isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void npmUnpublishSingleVersionWorksWhenMultipleArePublished() throws Exception {
        final String proj = "@hello/simple-npm-project";
        final String name = "simple-npm-project";
        final Key scnd = new Key.From(String.format("%s/-/%s-1.0.2.tgz", proj, proj));
        new TestResource("storage").addFilesTo(this.storage, Key.ROOT);
        new TestResource(String.format("binaries/%s-1.0.2.tgz", name)).saveTo(this.storage, scnd);
        new TestResource("json/unpublish.json")
            .saveTo(this.storage, new Key.From(String.format("%s/meta.json", proj)));
        final String unpubl = String.format("%s@1.0.2", proj);
        MatcherAssert.assertThat(
            "Unpublish command is succeeded",
            this.exec("npm", "unpublish", unpubl, "--registry", this.url),
            new StringContains(String.format("- %s", unpubl))
        );
        MatcherAssert.assertThat(
            "Archive was removed",
            this.storage.exists(
                new Key.From(String.format("%s/-/%s-1.0.2.tgz", proj, name))
            ).join(),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Meta file was updated",
            new JsonFromMeta(this.storage, new Key.From(proj))
                .json().containsKey("1.0.2"),
            new IsEqual<>(false)
        );
    }

    @Test
    void npmUnpublishSingleVersionWorksWhenSingleIsPublished() throws Exception {
        final String proj = "@hello/simple-npm-project";
        new TestResource("storage").addFilesTo(this.storage, Key.ROOT);
        final String unpubl = String.format("%s@1.0.1", proj);
        MatcherAssert.assertThat(
            "Unpublish command is succeeded",
            this.exec("npm", "unpublish", unpubl, "--registry", this.url),
            new StringContains(String.format("- %s", unpubl))
        );
        MatcherAssert.assertThat(
            "Files were deleted",
            this.storage.list(Key.ROOT).join().size(),
            new IsEqual<>(0)
        );
    }

    private String exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s\n", String.join(" ", command));
        final Container.ExecResult res = this.cntn.execInContainer(command);
        Logger.debug(this, "STDOUT:\n%s\nSTDERR:\n%s", res.getStdout(), res.getStderr());
        return res.getStdout();
    }
}

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
import com.artipie.npm.misc.JsonFromPublisher;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * IT case for `npm deprecate` command.
 * @since 0.8
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@DisabledOnOs(OS.WINDOWS)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class NpmDeprecateIT {

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    /**
     * Vert.x used to create tested FileStorage.
     */
    private Vertx vertx;

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
     * Storage used as client-side data (for packages to publish).
     */
    private Storage data;

    /**
     * Storage used for repository data.
     */
    private Storage repo;

    @BeforeEach
    void setUp() throws Exception {
        this.data = new FileStorage(this.tmp);
        this.repo = new InMemoryStorage();
        this.vertx = Vertx.vertx();
        final int port = new RandomFreePort().value();
        this.url = String.format("http://host.testcontainers.internal:%d", port);
        this.server = new VertxSliceServer(
            this.vertx,
            new LoggingSlice(new NpmSlice(URI.create(this.url).toURL(), this.repo)),
            port
        );
        this.server.start();
        Testcontainers.exposeHostPorts(port);
        this.cntn = new GenericContainer<>("node:14-alpine")
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
    }

    @Test
    void addsDeprecation() throws Exception {
        final String pkg = "@hello/simple-npm-project";
        new TestResource("json/not_deprecated.json")
            .saveTo(this.repo, new Key.From(pkg, "meta.json"));
        final String msg = "Danger! Do not use!";
        MatcherAssert.assertThat(
            "Npm deprecate command was successful",
            this.exec("npm", "deprecate", pkg, msg, "--registry", this.url).getExitCode(),
            new IsEqual<>(0)
        );
        MatcherAssert.assertThat(
            "Metadata file was updates",
            new JsonFromPublisher(
                this.repo.value(new Key.From(pkg, "meta.json")).join()
            ).json().join(),
            new JsonHas(
                "versions",
                new JsonHas(
                    "1.0.1", new JsonHas("deprecated", new JsonValueIs(msg))
                )
            )
        );
    }

    @Test
    void installsWithDeprecationWarning() throws Exception {
        final String pkg = "@hello/simple-npm-project";
        new TestResource("json/deprecated.json")
            .saveTo(this.repo, new Key.From(pkg, "meta.json"));
        new TestResource(String.format("storage/%s/-/%s-1.0.1.tgz", pkg, pkg))
            .saveTo(this.repo, new Key.From(pkg, "-", String.format("%s-1.0.1.tgz", pkg)));
        MatcherAssert.assertThat(
            this.exec("npm", "install", pkg, "--registry", this.url).getStderr(),
            new StringContainsInOrder(
                Arrays.asList(
                    "WARN", "deprecated", "@hello/simple-npm-project@1.0.1: Danger! Do not use!"
                )
            )
        );
    }

    @Test
    void publishThenDeprecateAndInstallWithDeprecationFromDependency() throws Exception {
        final String proj = "@hello/simple-npm-project";
        final String withdep = "project-with-simple-dependency";
        new TestResource("simple-npm-project")
            .addFilesTo(this.data, new Key.From(String.format("tmp/%s", proj)));
        new TestResource(withdep)
            .addFilesTo(this.data, new Key.From(String.format("tmp/%s", withdep)));
        this.exec("npm", "publish", String.format("tmp/%s", proj), "--registry", this.url);
        this.exec("npm", "publish", String.format("tmp/%s", withdep), "--registry", this.url);
        final String msg = "Danger! Do not use!";
        this.exec("npm", "deprecate", proj, msg, "--registry", this.url);
        final Container.ExecResult res;
        res = this.exec("npm", "install", withdep, "--registry", this.url);
        MatcherAssert.assertThat(
            "Deprecation warn was shown",
            res.getStderr(),
            new StringContainsInOrder(
                Arrays.asList(
                    "WARN", "deprecated", "@hello/simple-npm-project@1.0.1: Danger! Do not use!"
                )
            )
        );
        MatcherAssert.assertThat(
            "Package was installed",
            res.getStdout(),
            new StringContainsInOrder(
                Arrays.asList(
                    "+ project-with-simple-dependency@1.0.0", "added 2 packages"
                )
            )
        );
    }

    private Container.ExecResult exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s\n", String.join(" ", command));
        final Container.ExecResult res = this.cntn.execInContainer(command);
        Logger.debug(this, "STDOUT:\n%s\nSTDERR:\n%s", res.getStdout(), res.getStderr());
        return res;
    }

}

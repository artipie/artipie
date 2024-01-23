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
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.TokenAuthentication;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.npm.http.NpmSlice;
import com.artipie.security.policy.PolicyByUsername;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

/**
 * Make sure the library is compatible with npm 9 cli tools and auth.
 *
 * @since 0.11
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
public final class Npm9AuthIT {

    /**
     * Temporary directory for all tests.
     * Junit fails to remove temp dir in this test, so here we create it ourselves.
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
     * Server.
     */
    private VertxSliceServer server;

    /**
     * Repository URL.
     */
    private String url;

    /**
     * Repository port.
     */
    private int port;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    @BeforeEach
    void setUp() throws Exception {
        this.tmp = Files.createTempDirectory("npm9-auth-test");
        this.vertx = Vertx.vertx();
        this.data = new FileStorage(this.tmp);
        this.port = new RandomFreePort().value();
        this.url = String.format("http://host.testcontainers.internal:%d", this.port);
        this.server = new VertxSliceServer(
            this.vertx,
            new LoggingSlice(
                new NpmSlice(
                    URI.create(this.url).toURL(), new InMemoryStorage(),
                    new PolicyByUsername("Alice"),
                    new TestAuth(),
                    "test", Optional.empty()
                )
            ),
            this.port
        );
        this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        this.cntn = new GenericContainer<>("node:19-alpine")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
    }

    @AfterEach
    void tearDown() {
        FileUtils.deleteQuietly(this.tmp.toFile());
        this.server.stop();
        this.vertx.close();
        this.cntn.stop();
    }

    @Test
    void aliceCanInstallPublishedProject() throws Exception {
        this.data.save(
            new Key.From(".npmrc"),
            new Content.From(
                String.format("//host.testcontainers.internal:%d/:_authToken=abc123", this.port)
                    .getBytes(StandardCharsets.UTF_8)
            )
        ).join();
        new TestResource("simple-npm-project").addFilesTo(
            this.data, new Key.From("tmp/@hello/simple-npm-project")
        );
        this.exec("npm", "publish", "tmp/@hello/simple-npm-project/", "--registry", this.url);
        MatcherAssert.assertThat(
            this.exec("npm", "install", "@hello/simple-npm-project", "--registry", this.url),
            new StringContains("added 1 package")
        );
    }

    @Test
    void cannotInstallAndPushWithWrongToken() throws Exception {
        this.data.save(
            new Key.From(".npmrc"),
            new Content.From(
                String.format("//host.testcontainers.internal:%d/:_authToken=xyz098", this.port)
                    .getBytes(StandardCharsets.UTF_8)
            )
        ).join();
        new TestResource("simple-npm-project").addFilesTo(
            this.data, new Key.From("tmp/@hello/simple-npm-project")
        );
        MatcherAssert.assertThat(
            this.exec("npm", "publish", "tmp/@hello/simple-npm-project/", "--registry", this.url),
            new StringContainsInOrder(Arrays.asList("401", "Unable to authenticate"))
        );
        MatcherAssert.assertThat(
            this.exec("npm", "install", "@hello/simple-npm-project", "--registry", this.url),
            new StringContainsInOrder(Arrays.asList("401", "Unable to authenticate"))
        );
    }

    private String exec(final String... command) throws Exception {
        final Container.ExecResult res = this.cntn.execInContainer(command);
        Logger.debug(this, "Command:\n%s\nResult:\n%s", String.join(" ", command), res.toString());
        return res.toString();
    }

    /**
     * Test token auth: if token is abc123 return user with name Alice, otherwise returns empty.
     * @since 0.11
     */
    private static final class TestAuth implements TokenAuthentication {

        @Override
        public CompletionStage<Optional<AuthUser>> user(final String token) {
            CompletionStage<Optional<AuthUser>> res =
                CompletableFuture.completedFuture(Optional.empty());
            if ("abc123".equals(token)) {
                res = CompletableFuture.completedFuture(
                    Optional.of(new AuthUser("Alice", "test"))
                );
            }
            return res;
        }
    }
}

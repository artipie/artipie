/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.hex;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.hex.http.HexSlice;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.Authentication;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.security.policy.Policy;
import com.artipie.security.policy.PolicyByUsername;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

/**
 * HexPM integration test.
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
final class HexITCase {
    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Test user.
     */
    private static final Pair<String, String> USER = new ImmutablePair<>("Aladdin", "openSesame");

    /**
     * Temporary directory for all tests.
     */
    @TempDir
    Path tmp;

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Storage.
     */
    private Storage storage;

    /**
     * Vertx slice server port.
     */
    private int port;

    @AfterAll
    static void close() {
        HexITCase.VERTX.close();
    }

    @Test
    @Disabled("https://github.com/artipie/artipie/issues/1464")
    void downloadDependency() throws IOException, InterruptedException {
        this.init(true);
        this.addArtifactToArtipie();
        MatcherAssert.assertThat(
            this.exec("mix", "hex.package", "fetch", "decimal", "2.0.0", "--repo=my_repo"),
            new StringContains(
                "decimal v2.0.0 downloaded to /var/kv/decimal-2.0.0.tar"
            )
        );
    }

    @Disabled
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void fetchDependencies(final boolean anonymous) throws IOException, InterruptedException {
        this.init(anonymous);
        this.addArtifactToArtipie();
        MatcherAssert.assertThat(
            "Get dependency for the first time",
            this.exec("mix", "deps.get"),
            new StringContains("New:  decimal 2.0.0")
        );
        MatcherAssert.assertThat(
            "Get dependency for the second time",
            this.exec("mix", "deps.get"),
            new StringContains("Unchanged:  decimal 2.0.0")
        );
    }

    //todo know how send data to container`s stdin
    @Disabled
    @Test
    void uploadsDependency() throws IOException, InterruptedException {
        this.init(false);
        this.exec("mix", "hex.user", "auth");
        MatcherAssert.assertThat(
            this.exec("mix", "hex.publish"),
            new StringContains(
                "Published kv v0.1.0"
            )
        );
    }

    void addHexAndRepoToContainer() throws IOException, InterruptedException {
        this.exec("mix", "local.hex", "--force");
        this.exec(
            "mix", "hex.repo", "add", "my_repo",
            String.format("http://host.testcontainers.internal:%d", this.port)
        );
    }

    @AfterEach
    void stopContainer() {
        this.server.close();
        this.cntn.stop();
    }

    @SuppressWarnings("resource")
    private void init(final boolean anonymous) throws IOException, InterruptedException {
        final Pair<Policy<?>, Authentication> auth = this.auth(anonymous);
        this.storage = new InMemoryStorage();
        this.server = new VertxSliceServer(
            HexITCase.VERTX,
            new LoggingSlice(
                new HexSlice(this.storage, auth.getKey(), auth.getValue(), Optional.empty(), "test")
            )
        );
        this.port = this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        this.cntn = new GenericContainer<>("elixir:1.13.4")
            .withClasspathResourceMapping(
                "/kv",
                "/var/kv",
                BindMode.READ_WRITE
            )
            .withWorkingDirectory("/var/kv")
            .withEnv("HEX_UNSAFE_REGISTRY", "1")
            .withEnv("HEX_NO_VERIFY_REPO_ORIGIN", "1")
            .withCommand("tail", "-f", "/dev/null");
        this.cntn.start();
        this.addHexAndRepoToContainer();
    }

    private String exec(final String... actions) throws IOException, InterruptedException {
        return this.cntn.execInContainer(actions).toString().replace("\n", "");
    }

    private void addArtifactToArtipie() {
        new TestResource("packages")
            .addFilesTo(this.storage, new Key.From("packages"));
        new TestResource("tarballs")
            .addFilesTo(this.storage, new Key.From("tarballs"));
    }

    private Pair<Policy<?>, Authentication> auth(final boolean anonymous) {
        final Pair<Policy<?>, Authentication> res;
        if (anonymous) {
            res = new ImmutablePair<>(Policy.FREE, (name, pswd) -> Optional.of(AuthUser.ANONYMOUS));
        } else {
            res = new ImmutablePair<>(
                new PolicyByUsername(HexITCase.USER.getKey()),
                new Authentication.Single(
                    HexITCase.USER.getKey(), HexITCase.USER.getValue()
                )
            );
        }
        return res;
    }
}

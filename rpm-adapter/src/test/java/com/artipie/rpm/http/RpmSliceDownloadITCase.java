/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.Authentication;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.rpm.Digest;
import com.artipie.rpm.NamingPolicy;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.Rpm;
import com.artipie.rpm.TestRpm;
import com.artipie.security.policy.Policy;
import com.artipie.security.policy.PolicyByUsername;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

/**
 * Integration test for {@link RpmSlice}.
 */
@DisabledOnOs(OS.WINDOWS)
final class RpmSliceDownloadITCase {

    /**
     * Repo config.
     */
    private static final RepoConfig CONFIG = new RepoConfig.Simple(
        Digest.SHA256, new NamingPolicy.HashPrefixed(Digest.SHA1), true
    );

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
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Test storage.
     */
    private Storage asto;

    /**
     * Port.
     */
    private int port;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void installsByUrl() throws Exception {
        final TestRpm rpm = new TestRpm.Time();
        rpm.put(this.asto);
        this.start(Policy.FREE, (name, pswd) -> Optional.of(AuthUser.ANONYMOUS));
        MatcherAssert.assertThat(
            this.yumInstall(
                String.format(
                    "http://host.testcontainers.internal:%d/%s.rpm",
                    this.port, rpm.name()
                )
            ),
            new StringContainsInOrder(new ListOf<>(rpm.name(), "Complete!"))
        );
    }

    @Test
    void installsByUrlWithAuth() throws Exception {
        final String john = "john";
        final String pswd = "123";
        final TestRpm rpm = new TestRpm.Time();
        rpm.put(this.asto);
        this.start(
            new PolicyByUsername(john),
            new Authentication.Single(john, pswd)
        );
        MatcherAssert.assertThat(
            this.yumInstall(
                String.format(
                    "http://%s:%s@host.testcontainers.internal:%d/%s.rpm", john,
                    pswd, this.port, rpm.name()
                )
            ),
            new StringContainsInOrder(new ListOf<>(rpm.name(), "Complete!"))
        );
    }

    @Test
    void installsFromRepoWithSubDirs() throws IOException, InterruptedException {
        new TestRpm.Aspell().put(new SubStorage(new Key.From("spelling"), this.asto));
        new TestRpm.Time().put(this.asto);
        new Rpm(this.asto, RpmSliceDownloadITCase.CONFIG).batchUpdate(Key.ROOT).blockingAwait();
        this.start(Policy.FREE, (name, pswd) -> Optional.of(AuthUser.ANONYMOUS));
        final Path setting = this.tmp.resolve("example.repo");
        this.tmp.resolve("example.repo").toFile().createNewFile();
        Files.write(
            setting,
            new ListOf<>(
                "[example]",
                "name=Example Repository",
                String.format("baseurl=http://host.testcontainers.internal:%d/", this.port),
                "enabled=1",
                "gpgcheck=0"
            )
        );
        this.cntn.execInContainer("mv", "/home/example.repo", "/etc/yum.repos.d/");
        MatcherAssert.assertThat(
            this.cntn.execInContainer(
                "dnf", "-y", "repository-packages", "example", "install"
            ).toString(),
            new StringContainsInOrder(
                new ListOf<>(
                    "Installed", "aspell-12:0.60.6.1-9.el7.x86_64",
                    "time-1.7-45.el7.x86_64", "Complete!"
                )
            )
        );
    }

    @AfterEach
    void stopContainer() {
        this.server.close();
        this.cntn.stop();
    }

    @AfterAll
    static void close() {
        RpmSliceDownloadITCase.VERTX.close();
    }

    private String yumInstall(final String url) throws IOException, InterruptedException {
        return this.cntn.execInContainer(
            "dnf", "-y", "install", url
        ).getStdout();
    }

    private void start(final Policy<?> perms, final Authentication auth) {
        this.server = new VertxSliceServer(
            RpmSliceDownloadITCase.VERTX,
            new LoggingSlice(new RpmSlice(this.asto, perms, auth, RpmSliceDownloadITCase.CONFIG))
        );
        this.port = this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        this.cntn = new GenericContainer<>("fedora:36")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
    }

}

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
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.TokenAuthentication;
import com.artipie.http.auth.Tokens;
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
import java.util.concurrent.CompletableFuture;
import org.apache.commons.io.FileUtils;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

/**
 * Conda adapter integration test.
 * @since 0.5
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
public final class CondaSliceAuthITCase {

    /**
     * Test auth token.
     */
    private static final String TKN = "abc123";

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Test username.
     */
    private static final String UNAME = "Alice";

    /**
     * Test username.
     */
    private static final String PSWD = "wonderland";

    /**
     * Condarc file name with user credentials.
     */
    private static final String USER = ".condarc_user";

    /**
     * Condarc file name without credentials.
     */
    private static final String ANONIM = ".condarc_anonim";

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

    @BeforeEach
    void initialize() throws Exception {
        this.port = new RandomFreePort().get();
        this.storage = new InMemoryStorage();
        final String url = String.format("http://host.testcontainers.internal:%d", this.port);
        this.server = new VertxSliceServer(
            CondaSliceAuthITCase.VERTX,
            new LoggingSlice(
                new CondaSlice(
                    this.storage,
                    new PolicyByUsername(CondaSliceAuthITCase.UNAME),
                    new Authentication.Single(
                        CondaSliceAuthITCase.UNAME, CondaSliceAuthITCase.PSWD
                    ),
                    new FakeAuthTokens(),
                    url,
                    "any",
                    Optional.empty()
                )
            ),
            this.port
        );
        this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        Files.write(
            this.tmp.resolve(CondaSliceAuthITCase.USER),
            String.format(
                "channels:\n  - \"http://%s:%s@host.testcontainers.internal:%d\"",
                CondaSliceAuthITCase.UNAME, CondaSliceAuthITCase.PSWD, this.port
            ).getBytes()
        );
        Files.write(
            this.tmp.resolve(CondaSliceAuthITCase.ANONIM),
            String.format("channels:\n  - %s", url).getBytes()
        );
        FileUtils.copyFile(
            new TestResource("CondaSliceITCase/snappy-1.1.3-0.tar.bz2").asPath().toFile(),
            this.tmp.resolve("snappy-1.1.3-0.tar.bz2").toFile()
        );
        this.cntn = new GenericContainer<>("continuumio/miniconda3:22.11.1")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
        this.exec("conda", "install", "-y", "conda-build");
        this.exec("conda", "install", "-y", "conda-verify");
        this.exec("conda", "install", "-y", "anaconda-client");
    }

    @Test
    @Disabled("https://github.com/artipie/artipie/issues/1336")
    void canUploadAndInstall() throws Exception {
        this.moveCondarc(CondaSliceAuthITCase.ANONIM);
        this.exec(
            "anaconda", "config", "--set", "url",
            String.format("http://host.testcontainers.internal:%d/", this.port), "-s"
        );
        MatcherAssert.assertThat(
            "Anaconda login was not successful",
            this.exec(
                "anaconda", "-v", "login", "--username",
                CondaSliceAuthITCase.UNAME, "--password", CondaSliceAuthITCase.PSWD
            ),
            new StringContainsInOrder(
                new ListOf<>("http://host.testcontainers.internal", "Alice's login successful")
            )
        );
        MatcherAssert.assertThat(
            "Anaconda upload was not successful",
            this.exec(
                "anaconda", "upload", "./snappy-1.1.3-0.tar.bz2"
            ),
            new StringContainsInOrder(
                new ListOf<>(
                    "http://host.testcontainers.internal",
                    "Alice/snappy/1.1.3/linux-64/snappy-1.1.3-0.tar.bz2",
                    "Upload complete"
                )
            )
        );
        MatcherAssert.assertThat(
            "Failed to install package snappy",
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

    @Test
    void canInstallWithCondaInstall() throws Exception {
        this.moveCondarc(CondaSliceAuthITCase.USER);
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
    }

    private String exec(final String... command) throws Exception {
        final Container.ExecResult res = this.cntn.execInContainer(command);
        Logger.debug(this, "Command:\n%s\nResult:\n%s", String.join(" ", command), res.toString());
        return res.toString();
    }

    private void moveCondarc(final String name) throws IOException, InterruptedException {
        final String file = String.format("/home/%s", name);
        this.cntn.execInContainer("mv", file, "/root/.condarc");
        this.cntn.execInContainer("rm", file);
    }

    /**
     * Fake implementation of {@link Tokens}.
     * @since 0.5
     */
    static class FakeAuthTokens implements Tokens {

        @Override
        public TokenAuthentication auth() {
            return tkn -> {
                Optional<AuthUser> res = Optional.empty();
                if (CondaSliceAuthITCase.TKN.equals(tkn)) {
                    res = Optional.of(new AuthUser(CondaSliceAuthITCase.UNAME, "test"));
                }
                return CompletableFuture.completedFuture(res);
            };
        }

        @Override
        public String generate(final AuthUser user) {
            if (user.name().equals(CondaSliceAuthITCase.UNAME)) {
                return CondaSliceAuthITCase.TKN;
            }
            throw new IllegalStateException("Unexpected user");
        }
    }
}

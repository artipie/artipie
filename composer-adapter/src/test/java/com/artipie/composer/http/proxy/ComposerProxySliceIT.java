/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http.proxy;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.cache.Cache;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.composer.AllPackages;
import com.artipie.composer.AstoRepository;
import com.artipie.composer.test.ComposerSimple;
import com.artipie.composer.test.PackageSimple;
import com.artipie.composer.test.SourceServer;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.misc.RandomFreePort;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

/**
 * Integration test for {@link ComposerProxySlice}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@DisabledOnOs(OS.WINDOWS)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class ComposerProxySliceIT {
    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Jetty client.
     */
    private final JettyClientSlices client = new JettyClientSlices();

    /**
     * Temporary directory.
     */
    private Path tmp;

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
     * Server url.
     */
    private String url;

    /**
     * HTTP source server.
     */
    private SourceServer sourceserver;

    /**
     * Free port for starting source server.
     */
    private int sourceport;

    @BeforeEach
    void setUp() throws Exception {
        this.tmp = Files.createTempDirectory("");
        this.client.start();
        this.storage = new FileStorage(this.tmp);
        this.server = new VertxSliceServer(
            ComposerProxySliceIT.VERTX,
            new LoggingSlice(
                new ComposerProxySlice(
                    this.client,
                    URI.create("https://packagist.org"),
                    new AstoRepository(this.storage),
                    Authenticator.ANONYMOUS,
                    Cache.NOP
                )
            )
        );
        final int port = this.server.start();
        this.sourceport = new RandomFreePort().get();
        Testcontainers.exposeHostPorts(port, this.sourceport);
        this.cntn = new GenericContainer<>("composer:2.0.9")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
        this.url = String.format("http://host.testcontainers.internal:%s", port);
    }

    @AfterEach
    void tearDown() throws Exception {
        this.server.close();
        if (this.sourceserver != null) {
            this.sourceserver.close();
        }
        this.client.stop();
        this.cntn.stop();
        try {
            FileUtils.cleanDirectory(this.tmp.toFile());
            Files.deleteIfExists(this.tmp);
        } catch (final IOException ex) {
            Logger.error(this, "Failed to clean directory %[exception]s", ex);
        }
    }

    @AfterAll
    static void close() {
        ComposerProxySliceIT.VERTX.close();
    }

    @Test
    void installsPackageFromRemote() throws Exception {
        new ComposerSimple(this.url, "psr/log", "1.1.3")
            .writeTo(this.tmp.resolve("composer.json"));
        new TestResource("packages-remote.json")
            .saveTo(this.storage, new Key.From("packages.json"));
        MatcherAssert.assertThat(
            this.exec("composer", "install", "--verbose", "--no-cache"),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    new StringContains(false, "Installs: psr/log:1.1.3"),
                    new StringContains(false, "- Downloading psr/log (1.1.3)"),
                    new StringContains(
                        false,
                        "- Installing psr/log (1.1.3): Extracting archive"
                    )
                )
            )
        );
    }

    @Test
    void installsPackageFromLocal() throws Exception {
        final String name = "artipie/d8687716-47c1-4de6-a378-0557428fcce7";
        final String vers = "1.1.2";
        this.sourceserver = new SourceServer(ComposerProxySliceIT.VERTX, this.sourceport);
        new ComposerSimple(this.url, name, vers)
            .writeTo(this.tmp.resolve("composer.json"));
        final String pkg = new String(
            new PackageSimple(this.sourceserver.upload(), name).withSetVersion()
        );
        new BlockingStorage(this.storage).save(
            new AllPackages(),
            String.format(
                "{\"packages\":{\"%s\":{\"%s\":%s}}}", name, vers, pkg
            ).getBytes()
        );
        MatcherAssert.assertThat(
            this.exec("composer", "install", "--verbose", "--no-cache"),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    new StringContains(false, String.format("Installs: %s:%s", name, vers)),
                    new StringContains(false, String.format("- Downloading %s (%s)", name, vers)),
                    new StringContains(
                        false,
                        String.format("- Installing %s (%s): Extracting archive", name, vers)
                    )
                )
            )
        );
    }

    @Test
    void failsToInstallWhenPackageAbsent() throws Exception {
        final String name = "artipie/d8687716-47c1-4de6-a378-0557428fcce7";
        final String vers = "1.1.2";
        new ComposerSimple(this.url, name, vers)
            .writeTo(this.tmp.resolve("composer.json"));
        new TestResource("packages.json").saveTo(this.storage, new AllPackages());
        MatcherAssert.assertThat(
            this.exec("composer", "install", "--verbose", "--no-cache"),
            // @checkstyle LineLengthCheck (1 line)
            new StringContains(false, String.format("Root composer.json requires %s, it could not be found in any version", name))
        );
    }

    private String exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s\n", String.join(" ", command));
        final Container.ExecResult res = this.cntn.execInContainer(command);
        final String log = String.format(
            "STDOUT:\n%s\nSTDERR:\n%s", res.getStdout(), res.getStderr()
        );
        Logger.debug(this, log);
        return log;
    }
}

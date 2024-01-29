/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http.proxy;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.composer.AstoRepository;
import com.artipie.composer.misc.ContentAsJson;
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import javax.json.Json;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
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
 */
@DisabledOnOs(OS.WINDOWS)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class CacheComposerIT {
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
            CacheComposerIT.VERTX,
            new LoggingSlice(
                new ComposerProxySlice(
                    this.client,
                    URI.create("https://packagist.org"),
                    new AstoRepository(this.storage),
                    Authenticator.ANONYMOUS,
                    new ComposerStorageCache(new AstoRepository(this.storage))
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
        CacheComposerIT.VERTX.close();
    }

    @Test
    void installsPackageFromRemoteAndCachesIt() throws Exception {
        final String name = "psr/log";
        new ComposerSimple(this.url, name, "1.1.3")
            .writeTo(this.tmp.resolve("composer.json"));
        MatcherAssert.assertThat(
            "Installation failed",
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
        MatcherAssert.assertThat(
            "Index was not cached",
            this.storage.exists(
                new Key.From(ComposerStorageCache.CACHE_FOLDER, String.format("%s.json", name))
            ).join(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Info about cached package was not added to the cache file",
            new ContentAsJson(
                this.storage.value(CacheTimeControl.CACHE_FILE).join()
            ).value().toCompletableFuture().join()
            .containsKey(name),
            new IsEqual<>(true)
        );
    }

    @Test
    void installsPackageFromCache() throws Exception {
        final String name = "artipie/d8687716-47c1-4de6-a378-0557428fcce7";
        final String vers = "1.1.2";
        this.sourceserver = new SourceServer(CacheComposerIT.VERTX, this.sourceport);
        this.storage.save(
            CacheTimeControl.CACHE_FILE,
            new Content.From(
                Json.createObjectBuilder().add(
                    name,
                    ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).toString()
                ).build().toString()
                .getBytes()
            )
        ).join();
        new ComposerSimple(this.url, name, vers).writeTo(this.tmp.resolve("composer.json"));
        final byte[] pkg = new PackageSimple(this.sourceserver.upload(), name).withSetVersion();
        this.storage.save(
            new Key.From(ComposerStorageCache.CACHE_FOLDER, String.format("%s.json", name)),
            new Content.From(
                String.format(
                    "{\"packages\":{\"%s\":{\"%s\":%s}}}", name, vers, new String(pkg)
                ).getBytes()
            )
        ).join();
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

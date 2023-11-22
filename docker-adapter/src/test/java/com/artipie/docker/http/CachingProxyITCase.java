/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Digest;
import com.artipie.docker.Docker;
import com.artipie.docker.Manifests;
import com.artipie.docker.RepoName;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.cache.CacheDocker;
import com.artipie.docker.composite.MultiReadDocker;
import com.artipie.docker.composite.ReadWriteDocker;
import com.artipie.docker.junit.DockerClient;
import com.artipie.docker.junit.DockerClientSupport;
import com.artipie.docker.junit.DockerRepository;
import com.artipie.docker.proxy.ProxyDocker;
import com.artipie.docker.ref.ManifestRef;
import com.artipie.http.client.Settings;
import com.artipie.http.client.auth.AuthClientSlice;
import com.artipie.http.client.auth.GenericAuthenticator;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.google.common.base.Stopwatch;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Integration test for {@link ProxyDocker}.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DockerClientSupport
@DisabledOnOs(OS.WINDOWS)
final class CachingProxyITCase {

    /**
     * Example image to use in tests.
     */
    private Image img;

    /**
     * Docker client.
     */
    private DockerClient cli;

    /**
     * Docker cache.
     */
    private Docker cache;

    /**
     * HTTP client used for proxy.
     */
    private JettyClientSlices client;

    /**
     * Docker repository.
     */
    private DockerRepository repo;

    @BeforeEach
    void setUp() throws Exception {
        this.img = new Image.ForOs();
        this.client = new JettyClientSlices(new Settings.WithFollowRedirects(true));
        this.client.start();
        this.cache = new AstoDocker(new InMemoryStorage());
        final Docker local = new AstoDocker(new InMemoryStorage());
        this.repo = new DockerRepository(
            new ReadWriteDocker(
                new MultiReadDocker(
                    local,
                    new CacheDocker(
                        new MultiReadDocker(
                            new ProxyDocker(this.client.https("mcr.microsoft.com")),
                            new ProxyDocker(
                                new AuthClientSlice(
                                    this.client.https("registry-1.docker.io"),
                                    new GenericAuthenticator(this.client)
                                )
                            )
                        ),
                        this.cache, Optional.empty(), "*"
                    )
                ),
                local
            )
        );
        this.repo.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (this.repo != null) {
            this.repo.stop();
        }
        if (this.client != null) {
            this.client.stop();
        }
    }

    @Test
    void shouldPushAndPullLocal() throws Exception {
        final String original = this.img.remoteByDigest();
        this.cli.run("pull", original);
        final String image = String.format("%s/my-test/latest", this.repo.url());
        this.cli.run("tag", original, image);
        this.cli.run("push", image);
        this.cli.run("image", "rm", original);
        this.cli.run("image", "rm", image);
        final String output = this.cli.run("pull", image);
        MatcherAssert.assertThat(output, CachingProxyITCase.imagePulled(image));
    }

    @Test
    void shouldPullRemote() throws Exception {
        final String image = new Image.From(
            this.repo.url(), this.img.name(), this.img.digest(), this.img.layer()
        ).remoteByDigest();
        final String output = this.cli.run("pull", image);
        MatcherAssert.assertThat(output, CachingProxyITCase.imagePulled(image));
    }

    @Test
    @DisabledOnOs(OS.LINUX)
    void shouldPullWhenRemoteIsDown() throws Exception {
        final String image = new Image.From(
            this.repo.url(), this.img.name(), this.img.digest(), this.img.layer()
        ).remoteByDigest();
        this.cli.run("pull", image);
        this.awaitManifestCached();
        this.cli.run("image", "rm", image);
        this.client.stop();
        final String output = this.cli.run("pull", image);
        MatcherAssert.assertThat(output, CachingProxyITCase.imagePulled(image));
    }

    private void awaitManifestCached() throws Exception {
        final Manifests manifests = this.cache.repo(
            new RepoName.Simple(this.img.name())
        ).manifests();
        final ManifestRef ref = new ManifestRef.FromDigest(
            new Digest.FromString(this.img.digest())
        );
        final Stopwatch stopwatch = Stopwatch.createStarted();
        while (!manifests.get(ref).toCompletableFuture().join().isPresent()) {
            if (stopwatch.elapsed(TimeUnit.SECONDS) > TimeUnit.MINUTES.toSeconds(1)) {
                throw new IllegalStateException(
                    String.format(
                        "Manifest is expected to be present, but it was not found after %s seconds",
                        stopwatch.elapsed(TimeUnit.SECONDS)
                    )
                );
            }
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        }
    }

    private static Matcher<String> imagePulled(final String image) {
        return new StringContains(
            false,
            String.format("Status: Downloaded newer image for %s", image)
        );
    }
}

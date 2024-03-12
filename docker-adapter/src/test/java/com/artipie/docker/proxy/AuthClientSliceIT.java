/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.docker.ManifestReference;
import com.artipie.docker.RepoName;
import com.artipie.docker.manifest.Manifest;
import com.artipie.http.client.auth.AuthClientSlice;
import com.artipie.http.client.auth.GenericAuthenticator;
import com.artipie.http.client.jetty.JettyClientSlices;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * Integration test for {@link AuthClientSlice}.
 */
class AuthClientSliceIT {

    /**
     * HTTP client used for proxy.
     */
    private JettyClientSlices client;

    /**
     * Repository URL.
     */
    private AuthClientSlice slice;

    @BeforeEach
    void setUp() {
        this.client = new JettyClientSlices();
        this.client.start();
        this.slice = new AuthClientSlice(
            this.client.https("registry-1.docker.io"),
            new GenericAuthenticator(this.client)
        );
    }

    @AfterEach
    void tearDown() {
        this.client.stop();
    }

    @Test
    void getManifestByTag() {
        final RepoName name = new RepoName.Valid("library/busybox");
        final ProxyManifests manifests = new ProxyManifests(this.slice, name);
        final ManifestReference ref = ManifestReference.fromTag("latest");
        final Optional<Manifest> manifest = manifests.get(ref).toCompletableFuture().join();
        MatcherAssert.assertThat(
            manifest.isPresent(),
            new IsEqual<>(true)
        );
    }
}

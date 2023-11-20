/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.db;

import com.artipie.asto.misc.UncheckedSupplier;
import com.artipie.docker.Image;
import com.artipie.test.TestDeployment;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for artifact metadata
 * database.
 * @since 0.31
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class MetadataDockerITCase {

    /**
     * Deployment for tests.
     * @checkstyle VisibilityModifierCheck (5 lines)
     */
    @RegisterExtension
    final TestDeployment deployment = new TestDeployment(
        () -> new TestDeployment.ArtipieContainer().withConfig("artipie-db.yaml")
            .withRepoConfig("docker/registry.yml", "registry")
            .withRepoConfig("docker/docker-proxy-port.yml", "my-docker-proxy")
            .withUser("security/users/alice.yaml", "alice")
            .withExposedPorts(8081),
        () -> new TestDeployment.ClientContainer("alpine:3.11")
            .withPrivilegedMode(true)
            .withWorkingDirectory("/w")
    );

    @BeforeEach
    void setUp() throws Exception {
        this.deployment.setUpForDockerTests(8080, 8081);
    }

    @Test
    void pushAndPull(final @TempDir Path temp) throws Exception {
        final String alpine = "artipie:8080/registry/alpine:3.11";
        final String debian = "artipie:8080/registry/debian:stable-slim";
        new TestDeployment.DockerTest(this.deployment, "artipie:8080")
            .loginAsAlice()
            .pull("alpine:3.11")
            .tag("alpine:3.11", alpine)
            .push(alpine)
            .remove(alpine)
            .pull(alpine)
            .pull("debian:stable-slim")
            .tag("debian:stable-slim", debian)
            .push(debian)
            .remove(debian)
            .pull(debian)
            .assertExec();
        MetadataMavenITCase.awaitDbRecords(
            this.deployment, temp, rs -> new UncheckedSupplier<>(() -> rs.getInt(1) == 2).get()
        );
    }

    @Test
    void shouldPullFromProxy(final @TempDir Path temp) throws Exception {
        final Image image = new Image.ForOs();
        final String img = new Image.From(
            "artipie:8081",
            String.format("my-docker-proxy/%s", image.name()),
            image.digest(),
            image.layer()
        ).remoteByDigest();
        new TestDeployment.DockerTest(this.deployment, "artipie:8081")
            .loginAsAlice()
            .pull(img)
            .assertExec();
        MetadataMavenITCase.awaitDbRecords(
            this.deployment, temp, rs -> new UncheckedSupplier<>(() -> rs.getInt(1) == 1).get()
        );
    }
}

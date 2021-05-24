/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker;

import com.artipie.ArtipieServer;
import com.artipie.RepoConfigYaml;
import com.artipie.RepoPerms;
import com.artipie.docker.junit.DockerClient;
import com.artipie.docker.junit.DockerClientSupport;
import com.artipie.management.RepoPermissions;
import java.nio.file.Path;
import java.util.Arrays;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for auth in local Docker repositories.
 *
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DockerClientSupport
@Disabled
final class DockerLocalAuthIT {

    /**
     * Example docker image to use in tests.
     */
    private Image image;

    /**
     * Docker client.
     */
    private DockerClient client;

    /**
     * Tested Artipie server.
     */
    private ArtipieServer server;

    /**
     * Docker repository.
     */
    private String repository;

    @BeforeEach
    void setUp(@TempDir final Path root) throws Exception {
        this.server = new ArtipieServer(
            root, "my-docker",
            new RepoConfigYaml("docker").withFileStorage(root.resolve("data"))
                .withPermissions(
                    new RepoPerms(
                        new ListOf<>(
                            new RepoPermissions.PermissionItem(
                                ArtipieServer.ALICE.name(), new ListOf<>("read", "write")
                            ),
                            new RepoPermissions.PermissionItem(
                                ArtipieServer.BOB.name(), new ListOf<>("read")
                            )
                        )
                    )
                )
        );
        final int port = this.server.start();
        this.repository = String.format("localhost:%d", port);
        this.image = this.prepareImage();
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
    }

    @Test
    void shouldPush() throws Exception {
        this.login(ArtipieServer.ALICE);
        final String output = this.client.run("push", this.image.remote());
        MatcherAssert.assertThat(
            output,
            new AllOf<>(
                Arrays.asList(
                    new StringContains(String.format("%s: Pushed", this.image.layer())),
                    new StringContains(String.format("latest: digest: %s", this.image.digest()))
                )
            )
        );
    }

    @Test
    void shouldFailPushIfNoWritePermission() throws Exception {
        this.login(ArtipieServer.BOB);
        final DockerClient.Result result = this.client.runUnsafe("push", this.image.remote());
        MatcherAssert.assertThat(
            "Return code is not 0",
            result.returnCode(),
            new IsNot<>(new IsEqual<>(0))
        );
        MatcherAssert.assertThat(
            "Error reported",
            result.output(),
            new StringContains("denied")
        );
    }

    @Test
    void shouldFailPushIfAnonymous() throws Exception {
        this.logout();
        final DockerClient.Result result = this.client.runUnsafe("push", this.image.remote());
        MatcherAssert.assertThat(
            "Return code is not 0",
            result.returnCode(),
            new IsNot<>(new IsEqual<>(0))
        );
        MatcherAssert.assertThat(
            "Error reported",
            result.output(),
            new StringContains("no basic auth credentials")
        );
    }

    @Test
    @Disabled
    void shouldPullPushed() throws Exception {
        this.login(ArtipieServer.ALICE);
        this.client.run("push", this.image.remote());
        this.client.run("image", "rm", this.image.name());
        this.client.run("image", "rm", this.image.remote());
        this.login(ArtipieServer.BOB);
        final String output = this.client.run("pull", this.image.remote());
        MatcherAssert.assertThat(
            output,
            new StringContains(
                String.format("Status: Downloaded newer image for %s", this.image.remote())
            )
        );
    }

    @Test
    void shouldFailPullIfNoReadPermission() throws Exception {
        this.login(ArtipieServer.ALICE);
        this.client.run("push", this.image.remote());
        this.client.run("image", "rm", this.image.name());
        this.client.run("image", "rm", this.image.remote());
        this.login(ArtipieServer.CAROL);
        final DockerClient.Result result = this.client.runUnsafe("pull", this.image.remote());
        MatcherAssert.assertThat(
            "Return code is not 0",
            result.returnCode(),
            new IsNot<>(new IsEqual<>(0))
        );
        MatcherAssert.assertThat(
            "Error reported",
            result.output(),
            new StringContains("denied")
        );
    }

    @Test
    void shouldFailPullIfAnonymous() throws Exception {
        this.login(ArtipieServer.ALICE);
        this.client.run("push", this.image.remote());
        this.client.run("image", "rm", this.image.name());
        this.client.run("image", "rm", this.image.remote());
        this.logout();
        final DockerClient.Result result = this.client.runUnsafe("pull", this.image.remote());
        MatcherAssert.assertThat(
            "Return code is not 0",
            result.returnCode(),
            new IsNot<>(new IsEqual<>(0))
        );
        MatcherAssert.assertThat(
            "Error reported",
            result.output(),
            new StringContains("no basic auth credentials")
        );
    }

    private Image prepareImage() throws Exception {
        this.login(ArtipieServer.ALICE);
        final Image source = new Image.ForOs();
        this.client.run("pull", source.remoteByDigest());
        final String local = "my-docker/my-test";
        this.client.run("tag", source.remoteByDigest(), String.format("%s:latest", local));
        final Image img = new Image.From(
            this.repository,
            local,
            source.digest(),
            source.layer()
        );
        this.client.run("tag", source.remoteByDigest(), img.remote());
        return img;
    }

    private void login(final ArtipieServer.User user) throws Exception {
        this.client.login(user.name(), user.password(), this.repository);
    }

    private void logout() throws Exception {
        this.client.run("logout", this.repository);
    }
}

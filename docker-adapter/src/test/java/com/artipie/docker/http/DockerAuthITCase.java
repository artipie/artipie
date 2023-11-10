/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.junit.DockerClient;
import com.artipie.docker.junit.DockerClientSupport;
import com.artipie.docker.junit.DockerRepository;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.security.policy.PolicyByUsername;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test for authentication in {@link DockerSlice}.
 *
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @since 0.4
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DockerClientSupport
final class DockerAuthITCase {

    /**
     * Docker client.
     */
    private DockerClient cli;

    /**
     * Docker repository.
     */
    private DockerRepository repo;

    @BeforeEach
    void setUp() throws Exception {
        final TestAuthentication.User user = TestAuthentication.ALICE;
        this.repo = new DockerRepository(
            new DockerSlice(
                new AstoDocker(new InMemoryStorage()),
                new PolicyByUsername(user.name()),
                new BasicAuthScheme(new TestAuthentication()),
                Optional.empty(),
                "*"
            )
        );
        this.repo.start();
        this.cli.run(
            "login",
            "--username", user.name(),
            "--password", user.password(),
            this.repo.url()
        );
    }

    @AfterEach
    void tearDown() {
        this.repo.stop();
    }

    @Test
    void shouldPush() throws Exception {
        final Image original = new Image.ForOs();
        final String image = this.copy(original);
        final String output = this.cli.run("push", image);
        MatcherAssert.assertThat(
            output,
            new StringContains(
                false,
                String.format("latest: digest: %s", original.digest())
            )
        );
    }

    @Test
    void shouldPull() throws Exception {
        final String image = this.copy(new Image.ForOs());
        this.cli.run("push", image);
        this.cli.run("image", "rm", image);
        final String output = this.cli.run("pull", image);
        MatcherAssert.assertThat(
            output,
            new StringContains(
                false,
                String.format("Status: Downloaded newer image for %s", image)
            )
        );
    }

    private String copy(final Image original) throws Exception {
        this.cli.run("pull", original.remoteByDigest());
        final String copy = String.format("%s/my-test/latest", this.repo.url());
        this.cli.run("tag", original.remoteByDigest(), copy);
        return copy;
    }
}

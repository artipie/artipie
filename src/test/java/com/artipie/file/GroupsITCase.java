/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.file;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Integration test with user's group permissions.
 * @since 0.26
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class GroupsITCase {

    /**
     * Deployment for tests.
     * @checkstyle VisibilityModifierCheck (5 lines)
     * @checkstyle MagicNumberCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment deployment = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("binary/bin-user-groups.yml", "bin")
            .withCredentials("_credentials.yaml"),
        () -> new TestDeployment.ClientContainer("alpine:3.11")
            .withWorkingDirectory("/w")
    );

    @BeforeEach
    void setUp() throws Exception {
        this.deployment.assertExec(
            "Failed to install deps",
            new ContainerResultMatcher(),
            "apk", "add", "--no-cache", "curl"
        );
    }

    @Test
    void readersAndAdminsCanDownload() throws Exception {
        final byte[] target = new byte[]{0, 1, 2, 3};
        this.deployment.putBinaryToArtipie(
            target, "/var/artipie/data/bin/target"
        );
        this.deployment.assertExec(
            "Bob failed to download artifact",
            new ContainerResultMatcher(
                ContainerResultMatcher.SUCCESS, new StringContains("200")
            ),
            "curl", "-v", "-X", "GET", "--user", "bob:qwerty", "http://artipie:8080/bin/target"
        );
        this.deployment.assertExec(
            "John failed to download artifact",
            new ContainerResultMatcher(
                ContainerResultMatcher.SUCCESS, new StringContains("200")
            ),
            "curl", "-v", "-X", "GET", "--user", "john:xyz", "http://artipie:8080/bin/target"
        );
    }

    @Test
    void readersCanNotUpload() throws IOException {
        this.deployment.assertExec(
            "Upload should fail with 403 status",
            new ContainerResultMatcher(
                ContainerResultMatcher.SUCCESS, new StringContains("403 Forbidden")
            ),
            "curl", "-v", "-X", "PUT", "--user", "bob:qwerty", "--data-binary", "123",
            "http://artipie:8080/bin/target"
        );
    }

    @Test
    void adminsCanUpload() throws IOException {
        this.deployment.assertExec(
            "Failed to upload",
            new ContainerResultMatcher(
                ContainerResultMatcher.SUCCESS, new StringContains("201")
            ),
            "curl", "-v", "-X", "PUT", "--user", "john:xyz", "--data-binary", "123",
            "http://artipie:8080/bin/target"
        );
    }
}

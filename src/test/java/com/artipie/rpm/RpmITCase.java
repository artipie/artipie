/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import org.cactoos.list.ListOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.BindMode;

/**
 * IT case for RPM repository.
 * @since 0.12
 * @todo #1041:30min Add test cases with repository on individual port: create one more
 *  repository with `port` settings and start it in Artipie container exposing the port with
 *  `withExposedPorts` method. Then, parameterize test cases to check repositories with different
 *  ports. Check `FileITCase` as an example.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
public final class RpmITCase {

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("rpm/my-rpm.yml", "my-rpm"),
        () -> new TestDeployment.ClientContainer("fedora:35")
            .withClasspathResourceMapping(
                "rpm/time-1.7-45.el7.x86_64.rpm", "/w/time-1.7-45.el7.x86_64.rpm",
                BindMode.READ_ONLY
            )
    );

    @BeforeEach
    void setUp() throws IOException {
        this.containers.assertExec(
            "Dnf install curl failed", new ContainerResultMatcher(), "dnf", "-y", "install", "curl"
        );
        this.containers.putBinaryToClient(
            String.join(
                "\n", "[example]",
                "name=Example Repository",
                "baseurl=http://artipie:8080/my-rpm",
                "enabled=1",
                "gpgcheck=0"
            ).getBytes(),
            "/etc/yum.repos.d/example.repo"
        );
    }

    @Test
    void uploadsAndInstallsThePackage() throws Exception {
        this.containers.assertExec(
            "Failed to upload rpm package",
            new ContainerResultMatcher(),
            "curl", "http://artipie:8080/my-rpm/time-1.7-45.el7.x86_64.rpm",
            "--upload-file", "/w/time-1.7-45.el7.x86_64.rpm"
        );
        // @checkstyle MagicNumberCheck (1 line)
        Thread.sleep(2000);
        this.containers.assertExec(
            "Failed to install time package",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContainsInOrder(new ListOf<>("time-1.7-45.el7.x86_64", "Complete!"))
            ),
            "dnf", "-y", "repository-packages", "example", "install"
        );
    }

}

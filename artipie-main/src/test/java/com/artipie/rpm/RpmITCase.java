/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import org.cactoos.list.ListOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.BindMode;

/**
 * IT case for RPM repository.
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
public final class RpmITCase {

    /**
     * Test deployments.
             */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("rpm/my-rpm.yml", "my-rpm")
            .withRepoConfig("rpm/my-rpm-port.yml", "my-rpm-port")
            .withExposedPorts(8081),
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
    }

    @ParameterizedTest
    @CsvSource({
        "8080,my-rpm",
        "8081,my-rpm-port"
    })
    void uploadsAndInstallsThePackage(final String port, final String repo) throws Exception {
        this.containers.putBinaryToClient(
            String.join(
                "\n", "[example]",
                "name=Example Repository",
                String.format("baseurl=http://artipie:%s/%s", port, repo),
                "enabled=1",
                "gpgcheck=0"
            ).getBytes(),
            "/etc/yum.repos.d/example.repo"
        );
        this.containers.assertExec(
            "Failed to upload rpm package",
            new ContainerResultMatcher(),
            "curl",
            String.format("http://artipie:%s/%s/time-1.7-45.el7.x86_64.rpm", port, repo),
            "--upload-file", "/w/time-1.7-45.el7.x86_64.rpm"
        );
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

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.hexpm;

import com.artipie.asto.test.TestResource;
import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.BindMode;

/**
 * Integration tests for HexPm repository.
 *
 * @since 0.26
 */
@DisabledOnOs(OS.WINDOWS)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class HexpmITCase {
    /**
     * Artifact in tar format.
     */
    private static final String TAR = "decimal-2.0.0.tar";

    /**
     * Package info.
     */
    private static final String PACKAGE = "decimal";

    /**
     * Test deployments.
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("hexpm/hexpm.yml", "my-hexpm")
            .withExposedPorts(8080),
        () -> new TestDeployment.ClientContainer("elixir:1.13.4")
            .withEnv("HEX_UNSAFE_REGISTRY", "1")
            .withEnv("HEX_NO_VERIFY_REPO_ORIGIN", "1")
            .withWorkingDirectory("/w")
            .withClasspathResourceMapping(
                "hexpm/kv", "/w/kv", BindMode.READ_ONLY
            )
            .withClasspathResourceMapping(
                String.format("hexpm/%s", HexpmITCase.TAR),
                String.format("w/artifact/%s", HexpmITCase.TAR),
                BindMode.READ_ONLY
            )
    );

    @Test
    void pushArtifact() throws IOException {
        this.containers.assertExec(
            "Failed to upload artifact",
            new ContainerResultMatcher(ContainerResultMatcher.SUCCESS),
            "curl", "-X", "POST",
            "--data-binary", String.format("@./artifact/%s", HexpmITCase.TAR),
            "http://artipie:8080/my-hexpm/publish?replace=false"
        );
        this.containers.assertArtipieContent(
            "Package was not added to storage",
            String.format("/var/artipie/data/my-hexpm/packages/%s", HexpmITCase.PACKAGE),
            new IsEqual<>(
                new TestResource(String.format("hexpm/%s", HexpmITCase.PACKAGE)).asBytes()
            )
        );
        this.containers.assertArtipieContent(
            "Artifact was not added to storage",
            String.format("/var/artipie/data/my-hexpm/tarballs/%s", HexpmITCase.TAR),
            new IsEqual<>(new TestResource(String.format("hexpm/%s", HexpmITCase.TAR)).asBytes())
        );
    }

    @Test
    @Disabled("https://github.com/artipie/artipie/issues/1464")
    void downloadArtifact() throws Exception {
        this.containers.putResourceToArtipie(
            String.format("hexpm/%s", HexpmITCase.PACKAGE),
            String.format("/var/artipie/data/my-hexpm/packages/%s", HexpmITCase.PACKAGE)
        );
        this.containers.putResourceToArtipie(
            String.format("hexpm/%s", HexpmITCase.TAR),
            String.format("/var/artipie/data/my-hexpm/tarballs/%s", HexpmITCase.TAR)
        );
        this.addHexAndRepoToContainer();
        this.containers.assertExec(
            "Failed to download artifact",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContains(
                    String.format(
                        "%s v2.0.0 downloaded to /w/%s",
                        HexpmITCase.PACKAGE,
                        HexpmITCase.TAR
                    )
                )
            ),
            "mix", "hex.package", "fetch", HexpmITCase.PACKAGE, "2.0.0", "--repo=my_repo"
        );
    }

    private void addHexAndRepoToContainer() throws IOException {
        this.containers.clientExec("mix", "local.hex", "--force");
        this.containers.clientExec(
            "mix", "hex.repo", "add", "my_repo", "http://artipie:8080/my-hexpm"
        );
    }

}

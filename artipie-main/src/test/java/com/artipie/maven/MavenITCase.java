/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven;

import com.artipie.asto.test.TestResource;
import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.BindMode;

/**
 * Integration tests for Maven repository.
 * @since 0.11
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UseObjectForClearerAPI"})
@DisabledOnOs(OS.WINDOWS)
public final class MavenITCase {

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     * @checkstyle MagicNumberCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("maven/maven.yml", "my-maven")
            .withRepoConfig("maven/maven-port.yml", "my-maven-port")
            .withExposedPorts(8081),
        () -> new TestDeployment.ClientContainer("maven:3.6.3-jdk-11")
            .withWorkingDirectory("/w")
            .withClasspathResourceMapping(
                "maven/maven-settings.xml", "/w/settings.xml", BindMode.READ_ONLY
            )
            .withClasspathResourceMapping(
                "maven/maven-settings-port.xml", "/w/settings-port.xml", BindMode.READ_ONLY
            )
    );

    @ParameterizedTest
    @CsvSource({
        "helloworld,0.1,settings.xml,my-maven",
        "snapshot,1.0-SNAPSHOT,settings.xml,my-maven",
        "helloworld,0.1,settings-port.xml,my-maven-port",
        "snapshot,1.0-SNAPSHOT,settings-port.xml,my-maven-port"
    })
    void downloadsArtifact(final String type, final String vers, final String stn,
        final String repo) throws Exception {
        final String meta = String.format("com/artipie/%s/maven-metadata.xml", type);
        this.containers.putResourceToArtipie(
            meta, String.format("/var/artipie/data/%s/%s", repo, meta)
        );
        final String base = String.format("com/artipie/%s/%s", type, vers);
        MavenITCase.getResourceFiles(base).stream().map(r -> String.join("/", base, r)).forEach(
            item -> this.containers.putResourceToArtipie(
                item, String.format("/var/artipie/data/%s/%s", repo, item)
            )
        );
        this.containers.assertExec(
            "Failed to get dependency",
            new ContainerResultMatcher(),
            "mvn", "-B", "-q", "-s", stn, "-e", "dependency:get",
            String.format("-Dartifact=com.artipie:%s:%s", type, vers)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "helloworld,0.1,settings.xml,pom.xml",
        "snapshot,1.0-SNAPSHOT,settings.xml,pom.xml",
        "helloworld,0.1,settings-port.xml,pom-port.xml",
        "snapshot,1.0-SNAPSHOT,settings-port.xml,pom-port.xml"
    })
    void deploysArtifact(final String type, final String vers, final String stn, final String pom)
        throws Exception {
        this.containers.putBinaryToClient(
            new TestResource(String.format("%s-src/%s", type, pom)).asBytes(), "/w/pom.xml"
        );
        this.containers.assertExec(
            "Deploy failed",
            new ContainerResultMatcher(ContainerResultMatcher.SUCCESS),
            "mvn", "-B", "-q", "-s", stn, "deploy", "-Dmaven.install.skip=true"
        );
        this.containers.assertExec(
            "Download failed",
            new ContainerResultMatcher(ContainerResultMatcher.SUCCESS),
            "mvn", "-B", "-q", "-s", stn, "-U", "dependency:get",
            String.format("-Dartifact=com.artipie:%s:%s", type, vers)
        );
    }

    /**
     * Get resource files.
     * @param path Resource path
     * @return List of subresources
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    static List<String> getResourceFiles(final String path) throws IOException {
        final List<String> filenames = new ArrayList<>(0);
        try (InputStream in = getResourceAsStream(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String resource;
            while ((resource = br.readLine()) != null) {
                filenames.add(resource);
            }
        }
        return filenames;
    }

    /**
     * Get resource stream.
     * @param resource Name
     * @return Stream
     */
    private static InputStream getResourceAsStream(final String resource) {
        return Optional.ofNullable(
            Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)
        ).or(
            () -> Optional.ofNullable(MavenITCase.class.getResourceAsStream(resource))
        ).orElseThrow(
            () -> new UncheckedIOException(
                new IOException(String.format("Resource `%s` not found", resource))
            )
        );
    }

}

/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.maven;

import com.artipie.asto.test.TestResource;
import com.artipie.test.TestDeployment;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container.ExecResult;

/**
 * Integration tests for Maven repository.
 * @since 0.11
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
public final class MavenITCase {

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("maven/maven.yml", "my-maven"),
        () -> new TestDeployment.ClientContainer("maven:3.6.3-jdk-11")
            .withWorkingDirectory("/w")
            .withClasspathResourceMapping(
                "maven/maven-settings.xml", "/w/settings.xml", BindMode.READ_ONLY
            )
    );

    @ParameterizedTest
    @CsvSource({"helloworld,0.1", "snapshot,1.0-SNAPSHOT"})
    void downloadsArtifact(final String type, final String vers) throws Exception {
        final String meta = String.format("com/artipie/%s/maven-metadata.xml", type);
        this.containers.putResourceToArtipie(
            meta, String.join("/", "/var/artipie/data/my-maven", meta)
        );
        final String base = String.format("com/artipie/%s/%s", type, vers);
        MavenITCase.getResourceFiles(base).stream().map(r -> String.join("/", base, r)).forEach(
            item -> this.containers.putResourceToArtipie(
                item, String.join("/", "/var/artipie/data/my-maven", item)
            )
        );
        this.containers.assertExec(
            "Failed to get dependency",
            new ContainerResultMatcher(Matchers.equalTo(0)),
            "mvn", "-B", "-q", "-s", "settings.xml", "-e", "dependency:get",
            String.format("-Dartifact=com.artipie:%s:%s", type, vers)
        );
    }

    @ParameterizedTest
    @CsvSource({"helloworld,0.1,0.1", "snapshot,1.0-SNAPSHOT"})
    void deploysArtifact(final String type, final String vers) throws Exception {
        this.containers.putBinaryToClient(
            new TestResource(String.format("%s-src/pom.xml", type)).asBytes(), "/w/pom.xml"
        );
        this.containers.assertExec(
            "Deploy failed",
            new ContainerResultMatcher(Matchers.is(0)),
            "mvn", "-B", "-q", "-s", "settings.xml",
            "deploy", "-Dmaven.install.skip=true"
        );
        this.containers.assertExec(
            "Download failed",
            new ContainerResultMatcher(Matchers.is(0)),
            "mvn", "-B", "-q", "-s", "settings.xml", "-U", "dependency:get",
            String.format("-Dartifact=com.artipie:%s:%s", type, vers)
        );
    }

    /**
     * Get resource files.
     * @param path Resource path
     * @return List of subresources
     * @todo #855:30min Refactor resource extracting
     *  Consider creating a method to scan resources from directory and
     *  bind it to container path. Also, move maven-related resources
     *  from test resource root directory to subfolder, e.g. `maven-it`.
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

    /**
     * Container exec result matcher.
     * @since 0.16
     * @todo #855:30min Move this class to test sources,
     *  add additional constructors for int status and default to expect zero status.
     *  Consider creating standard enum or public static fields for
     *  `SUCCESS` - exit code is `0` and
     *  `ERROR` - exit code is greater than `0`
     */
    public static final class ContainerResultMatcher extends TypeSafeMatcher<ExecResult> {

        /**
         * Expected status matcher.
         */
        private final Matcher<Integer> status;

        /**
         * Stdout matcher.
         */
        private final Matcher<String> stdout;

        /**
         * New matcher.
         * @param status Expected status
         * @param stdout Expected message in stdout
         */
        public ContainerResultMatcher(final Matcher<Integer> status, final Matcher<String> stdout) {
            this.status = status;
            this.stdout = stdout;
        }

        /**
         * New matcher.
         * @param status Expected status
         */
        public ContainerResultMatcher(final Matcher<Integer> status) {
            this(status, new StringContains(""));
        }

        /**
         * New default matcher with expected status 0.
         */
        public ContainerResultMatcher() {
            this(new IsEqual<>(0), new StringContains(""));
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("status ").appendDescriptionOf(this.status)
            .appendText("stdout ").appendDescriptionOf(this.stdout);
        }

        @Override
        public boolean matchesSafely(final ExecResult item) {
            return this.status.matches(item.getExitCode()) && this.stdout.matches(item.getStdout());
        }

        @Override
        public void describeMismatchSafely(final ExecResult res, final Description desc) {
            desc.appendText("failed with status:\n")
                .appendValue(res.getExitCode())
                .appendText("\nSTDOUT: ")
                .appendText(res.getStdout())
                .appendText("\nSTDERR: ")
                .appendText(res.getStderr())
                .appendText("\n");
        }
    }
}

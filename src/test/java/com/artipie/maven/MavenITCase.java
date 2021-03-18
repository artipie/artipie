/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.maven;

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
import org.junit.jupiter.api.Disabled;
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
 * @todo #855:30min Deploy test is broken, mvn deploy fails
 *  on 500 server respose due to file not exist exception at
 *  maven upload.
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
            .withRepoConfig("maven.yml", "my-maven"),
        () -> new TestDeployment.ClientContainer("maven:3.6.3-jdk-11")
            .withWorkingDirectory("/w")
            .withClasspathResourceMapping(
                "maven-settings.xml", "/w/settings.xml", BindMode.READ_ONLY
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
    @Disabled
    @CsvSource({"helloworld,0.1,0.1", "snapshot,1.0-SNAPSHOT"})
    void deploysArtifact(final String type, final String vers) throws Exception {
        this.containers.putResourceToArtipie(
            String.format("%s-src/pom.xml", type), "/w/pom.xml"
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
    private static List<String> getResourceFiles(final String path) throws IOException {
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
         * New matcher.
         * @param status Expected status
         */
        public ContainerResultMatcher(final Matcher<Integer> status) {
            this.status = status;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("status ").appendDescriptionOf(this.status);
        }

        @Override
        public boolean matchesSafely(final ExecResult item) {
            return this.status.matches(item.getExitCode());
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

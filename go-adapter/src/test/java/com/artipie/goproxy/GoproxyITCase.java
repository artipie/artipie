/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.goproxy;

import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration case for {@link Goproxy}.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@Testcontainers
@DisabledOnOs(OS.WINDOWS)
public final class GoproxyITCase {
    /**
     * Path to repo (will be used both in test and inside golang container).
     */
    private static Path repo;

    /**
     * GoLang container to verify Go repository layout.
     */
    private static GoContainer golang;

    @Test
    public void savesAndLoads() throws Exception {
        final Storage storage = new FileStorage(GoproxyITCase.repo);
        final Goproxy goproxy = new Goproxy(storage);
        goproxy.update("example.com/foo/bar", "0.0.123").blockingAwait();
        goproxy.update("example.com/foo/bar", "0.0.124").blockingAwait();
        this.validateResult(
            golang.execInContainer("go", "install", "-v"),
            "go: downloading example.com/foo/bar v0.0.124"
        );
        this.validateResult(
            golang.execInContainer("go", "run", "test.go"),
            "Hey, you!",
            "Works!!!"
        );
    }

    @BeforeAll
    static void startContainer() throws Exception {
        GoproxyITCase.repo = Paths.get(
            Thread.currentThread()
            .getContextClassLoader()
            .getResource("repo")
            .toURI()
        );
        GoproxyITCase.golang = new GoContainer()
            .withClasspathResourceMapping("repo", "/opt/repo", BindMode.READ_WRITE)
            .withClasspathResourceMapping("work", "/opt/work", BindMode.READ_WRITE)
            .withEnv("GOPROXY", "file:///opt/repo")
            .withEnv("GOSUMDB", "off")
            .withWorkingDirectory("/opt/work")
            .withCommand("tail", "-f", "/dev/null");
        GoproxyITCase.golang.start();
    }

    @AfterAll
    static void stopContainer() {
        GoproxyITCase.golang.stop();
    }

    private void validateResult(final Container.ExecResult result,
        final String... substrings) throws Exception {
        MatcherAssert.assertThat(0, new IsEqual<>(result.getExitCode()));
        MatcherAssert.assertThat(
            String.join("\n", result.getStdout(), result.getStderr()),
            new AllOf<>(
                Arrays.stream(substrings)
                    .map(StringContains::new)
                    .collect(Collectors.toList())
            )
        );
    }

    /**
     * Inner subclass to instantiate GoLang container.
     * @since 0.3
     */
    private static class GoContainer extends GenericContainer<GoContainer> {
        GoContainer() {
            super("golang:1.15.12");
        }
    }
}

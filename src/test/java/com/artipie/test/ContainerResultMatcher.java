/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.test;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.testcontainers.containers.Container;

/**
 * Container exec result matcher.
 * @since 0.20
 */
public final class ContainerResultMatcher extends TypeSafeMatcher<Container.ExecResult> {

    /**
     * Success exit code.
     */
    public static final int SUCCESS = 0;

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
     * New matcher.
     * @param status Expected status
     */
    public ContainerResultMatcher(final Integer status) {
        this(new IsEqual<>(status), new StringContains(""));
    }

    /**
     * New matcher.
     * @param status Expected status
     * @param stdout Expected message in stdout
     */
    public ContainerResultMatcher(final Integer status, final Matcher<String> stdout) {
        this(new IsEqual<>(status), stdout);
    }

    /**
     * New default matcher with expected status 0.
     */
    public ContainerResultMatcher() {
        this(new IsEqual<>(ContainerResultMatcher.SUCCESS), new StringContains(""));
    }

    @Override
    public void describeTo(final Description description) {
        description.appendText("status ").appendDescriptionOf(this.status)
        .appendText("stdout ").appendDescriptionOf(this.stdout);
    }

    @Override
    public boolean matchesSafely(final Container.ExecResult item) {
        return this.status.matches(item.getExitCode())
            && this.stdout.matches(String.format("%s\n%s", item.getStdout(), item.getStderr()));
    }

    @Override
    public void describeMismatchSafely(final Container.ExecResult res, final Description desc) {
        desc.appendText("failed with status:\n")
            .appendValue(res.getExitCode())
            .appendText("\nSTDOUT: ")
            .appendText(res.getStdout())
            .appendText("\nSTDERR: ")
            .appendText(res.getStderr())
            .appendText("\n");
    }
}

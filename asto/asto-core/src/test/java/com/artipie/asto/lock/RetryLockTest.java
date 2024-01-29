/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.lock;

import com.artipie.asto.FailedCompletionStage;
import com.google.common.base.Stopwatch;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.number.IsCloseTo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Test cases for {@link RetryLock}.
 *
 * @since 0.24
 */
@SuppressWarnings("PMD.ProhibitPlainJunitAssertionsRule")
@Timeout(3)
final class RetryLockTest {

    /**
     * Scheduler used in tests.
     */
    private ScheduledExecutorService scheduler;

    @BeforeEach
    void setUp() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @AfterEach
    void tearDown() {
        this.scheduler.shutdown();
    }

    @Test
    void shouldSucceedAcquireAfterSomeAttempts() {
        final int attempts = 2;
        final FailingLock mock = new FailingLock(attempts);
        new RetryLock(this.scheduler, mock).acquire().toCompletableFuture().join();
        MatcherAssert.assertThat(
            mock.acquire.invocations.size(),
            new IsEqual<>(attempts)
        );
    }

    @Test
    void shouldFailAcquireAfterMaxRetriesWithExtendingInterval() {
        final FailingLock mock = new FailingLock(5);
        final CompletionStage<Void> acquired = new RetryLock(this.scheduler, mock).acquire();
        Assertions.assertThrows(
            Exception.class,
            () -> acquired.toCompletableFuture().join(),
            "Fails to acquire"
        );
        assertRetryAttempts(mock.acquire.invocations);
    }

    @Test
    void shouldSucceedReleaseAfterSomeAttempts() {
        final int attempts = 2;
        final FailingLock mock = new FailingLock(attempts);
        new RetryLock(this.scheduler, mock).release().toCompletableFuture().join();
        MatcherAssert.assertThat(
            mock.release.invocations.size(),
            new IsEqual<>(attempts)
        );
    }

    @Test
    void shouldFailReleaseAfterMaxRetriesWithExtendingInterval() {
        final FailingLock mock = new FailingLock(5);
        final CompletionStage<Void> released = new RetryLock(this.scheduler, mock).release();
        Assertions.assertThrows(
            Exception.class,
            () -> released.toCompletableFuture().join(),
            "Fails to release"
        );
        assertRetryAttempts(mock.release.invocations);
    }

    private static void assertRetryAttempts(final List<Long> attempts) {
        MatcherAssert.assertThat(
            "Makes 3 attempts",
            attempts.size(), new IsEqual<>(3)
        );
        MatcherAssert.assertThat(
            "Makes 1st attempt almost instantly",
            attempts.get(0).doubleValue(),
            new IsCloseTo(0, 100)
        );
        MatcherAssert.assertThat(
            "Makes 2nd attempt in 500ms after 1st attempt",
            attempts.get(1).doubleValue() - attempts.get(0),
            new IsCloseTo(500, 100)
        );
        MatcherAssert.assertThat(
            "Makes 3rd attempt in 500ms * 1.5 = 750ms after 2nd",
            attempts.get(2).doubleValue() - attempts.get(1),
            new IsCloseTo(750, 100)
        );
    }

    /**
     * Lock failing acquire & release specified number of times before producing successful result.
     * Collects history of invocation timings.
     *
     * @since 0.24
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private static class FailingLock implements Lock {

        /**
         * Acquire task.
         */
        private final FailingTask acquire;

        /**
         * Release task.
         */
        private final FailingTask release;

        FailingLock(final int failures) {
            this.acquire = new FailingTask(failures);
            this.release = new FailingTask(failures);
        }

        @Override
        public CompletionStage<Void> acquire() {
            return this.acquire.invoke();
        }

        @Override
        public CompletionStage<Void> release() {
            return this.release.invoke();
        }
    }

    /**
     * Task failing specified number of times before producing successful result.
     * Collects history of invocation timings.
     *
     * @since 0.24
     */
    private static class FailingTask {

        /**
         * Number of failures before successful result.
         */
        private final int failures;

        /**
         * Invocations history.
         */
        private final List<Long> invocations;

        /**
         * Stopwatch to track invocation times.
         */
        private final Stopwatch stopwatch;

        FailingTask(final int failures) {
            this.failures = failures;
            this.invocations = new ArrayList<>(failures);
            this.stopwatch = Stopwatch.createStarted();
        }

        CompletionStage<Void> invoke() {
            synchronized (this.invocations) {
                this.invocations.add(this.stopwatch.elapsed(TimeUnit.MILLISECONDS));
                final CompletionStage<Void> result;
                if (this.invocations.size() < this.failures) {
                    result = new FailedCompletionStage<>(new RuntimeException());
                } else {
                    result = CompletableFuture.allOf();
                }
                return result;
            }
        }
    }
}

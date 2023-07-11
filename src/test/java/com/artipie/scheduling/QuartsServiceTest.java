/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.scheduling;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.awaitility.Awaitility;
import org.cactoos.list.ListOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

/**
 * Test for {@link QuartsService}.
 * @since 1.3
 * @checkstyle IllegalTokenCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
public final class QuartsServiceTest {

    /**
     * Quartz service to test.
     */
    private QuartsService service;

    @BeforeEach
    void init() {
        this.service = new QuartsService();
    }

    @Test
    void runsEventProcessorJobs() throws SchedulerException, InterruptedException {
        final TestConsumer first = new TestConsumer();
        final TestConsumer second = new TestConsumer();
        final TestConsumer third = new TestConsumer();
        final EventQueue<Character> queue = this.service.addPeriodicEventsProcessor(
            1, new ListOf<Consumer<Character>>(first, second, third)
        );
        this.service.start();
        for (char sym = 'a'; sym <= 'z'; sym++) {
            queue.put(sym);
            if ((int) sym % 5 == 0) {
                Thread.sleep(1500);
            }
        }
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
            .until(() -> first.cnt.get() + second.cnt.get() + third.cnt.get() == 26);
    }

    @Test
    void runsGivenJobs() throws SchedulerException {
        final AtomicInteger count = new AtomicInteger();
        final JobDataMap data = new JobDataMap();
        data.put("cnt", count);
        this.service.schedulePeriodicJob(2, 3, TestJob.class, data);
        this.service.start();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> count.get() > 12);
    }

    /**
     * Test consumer.
     * @since 1.3
     */
    static final class TestConsumer implements Consumer<Character> {

        /**
         * Count for accept method call.
         */
        private final AtomicInteger cnt = new AtomicInteger();

        @Override
        public void accept(final Character strings) {
            this.cnt.incrementAndGet();
        }
    }

    /**
     * Test job.
     * @since 1.3
     */
    public static final class TestJob implements Job {

        /**
         * Count.
         */
        private AtomicInteger cnt;

        @Override
        public void execute(final JobExecutionContext context) throws JobExecutionException {
            this.cnt.incrementAndGet();
        }

        /**
         * Set count.
         * @param count Count
         */
        public void setCnt(final AtomicInteger count) {
            this.cnt = count;
        }
    }

}

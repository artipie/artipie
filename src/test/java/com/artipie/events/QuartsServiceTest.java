/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.events;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.SchedulerException;

/**
 * Test for {@link QuartsService}.
 * @since 0.31
 * @checkstyle IllegalTokenCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
class QuartsServiceTest {

    /**
     * Quartz service to test.
     */
    private QuartsService service;

    @BeforeEach
    void init() {
        this.service = new QuartsService();
    }

    @Test
    void runsQuartsJobs() throws SchedulerException, InterruptedException {
        final TestConsumer consumer = new TestConsumer();
        final EventQueue<Character> queue = this.service.addPeriodicEventsProcessor(consumer, 3, 1);
        this.service.start();
        for (char sym = 'a'; sym <= 'z'; sym++) {
            queue.put(sym);
            if ((int) sym % 5 == 0) {
                Thread.sleep(1500);
            }
        }
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> consumer.cnt.get() == 26);
    }

    /**
     * Test consumer.
     * @since 0.31
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

}

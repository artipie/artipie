/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.jfr;

import com.artipie.asto.Content;
import com.artipie.asto.Splitting;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for ChunksAndSizeMetricsContent.
 *
 * @since 0.28.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle ParameterNameCheck (500 lines)
 * @checkstyle LocalFinalVariableNameCheck (500 lines)
 * @checkstyle IllegalCatchCheck (500 lines)
 */
@SuppressWarnings(
    {
        "PMD.AvoidCatchingGenericException",
        "PMD.AvoidThrowingRawExceptionTypes",
        "PMD.EmptyCatchBlock",
        "PMD.AvoidDuplicateLiterals"
    }
)
class ChunksAndSizeMetricsContentTest {

    /**
     * Random one for all tests.
     */
    private static final Random RANDOM = new Random();

    /**
     * To check chunk count.
     */
    private AtomicInteger count;

    /**
     * To check size of content`s data.
     */
    private AtomicLong sum;

    @BeforeEach
    void init() {
        this.count = new AtomicInteger();
        this.sum = new AtomicLong();
    }

    @Test
    void shouldPassToCallbackChunkCountAndReceivedBytesWithDelay() {
        final int size = 10 * 1024 * 1024;
        final int chunks = 15;
        Flowable.fromPublisher(
            new ChunksAndSizeMetricsContent(
                this.content(size, chunks, true),
                (c, w) -> {
                    this.count.set(c);
                    this.sum.set(w);
                }
            )
        ).blockingSubscribe();
        this.assertResults(chunks, size);
    }

    @Test
    void shouldPassToCallbackChunkCountAndReceivedBytesWithoutDelay() {
        final int size = 5 * 1024 * 1024;
        final int chunks = 24;
        Flowable.fromPublisher(
            new ChunksAndSizeMetricsContent(
                this.content(size, chunks, false),
                (c, w) -> {
                    this.count.set(c);
                    this.sum.set(w);
                }
            )
        ).blockingSubscribe();
        this.assertResults(chunks, size);
    }

    @Test
    void shouldPassToCallbackMinisOneChunkCountWhenErrorIsOccurred() {
        final byte[] data = new byte[2 * 1024];
        RANDOM.nextBytes(data);
        final AtomicBoolean called = new AtomicBoolean(false);
        final AtomicInteger counter = new AtomicInteger();
        try {
            final Content content = new Content.From(Flowable.fromPublisher(new Content.From(data))
                .flatMap(
                    buffer -> new Splitting(
                        buffer,
                        1024
                    ).publisher()
                ).filter(
                    buf -> {
                        if (called.compareAndSet(false, true)) {
                            throw new RuntimeException("Stop!");
                        }
                        return true;
                    }));
            Flowable.fromPublisher(
                new ChunksAndSizeMetricsContent(
                    content,
                    (c, w) -> {
                        MatcherAssert.assertThat(c, Is.is(-1));
                        MatcherAssert.assertThat(w, Is.is(0L));
                        counter.getAndIncrement();
                    }
                )
            ).blockingSubscribe();
            Assertions.fail();
        } catch (final RuntimeException err) {
            // @checkstyle MethodBodyCommentsCheck (1 lines)
            // No-op.
        }
        MatcherAssert.assertThat(counter.get(), Is.is(1));
    }

    /**
     * Asserts Chunks count & data size.
     *
     * @param chunks Chunks count.
     * @param size Size of data.
     */
    private void assertResults(final int chunks, final long size) {
        MatcherAssert.assertThat(this.count.get(), Is.is(chunks));
        MatcherAssert.assertThat(this.sum.get(), Is.is(size));
    }

    /**
     * Creates content.
     *
     * @param size Size of content's data.
     * @param chunks Chunks count.
     * @param withDelay Emulate  delay.
     * @return Content.
     */
    private Content content(final int size, final int chunks, final boolean withDelay) {
        final byte[] data = new byte[size];
        RANDOM.nextBytes(data);
        final int rest = size % chunks;
        final int chunkSize = size / chunks + rest;
        Flowable<ByteBuffer> flowable = Flowable.fromPublisher(new Content.From(data))
            .flatMap(
                buffer -> new Splitting(
                    buffer,
                    chunkSize
                ).publisher()
            );
        if (withDelay) {
            flowable = flowable.delay(RANDOM.nextInt(5_000), TimeUnit.MILLISECONDS);
        }
        return new Content.From(flowable);
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.micrometer;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ValueNotFoundException;
import com.artipie.asto.memory.InMemoryStorage;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.reactivex.Flowable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link MicrometerStorage}.
 * @since 0.28
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class MicrometerStorageTest {

    /**
     * Test key.
     */
    private static final Key KEY = new Key.From("any/test.txt");

    /**
     * Test registry.
     */
    private SimpleMeterRegistry registry;

    /**
     * Test storage instance.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.registry = new SimpleMeterRegistry();
        this.asto = new MicrometerStorage(new InMemoryStorage(), this.registry);
    }

    @Test
    void logsExistsOp() {
        this.asto.exists(MicrometerStorageTest.KEY).join();
        MatcherAssert.assertThat(
            this.registry.getMetersAsString(),
            Matchers.stringContainsInOrder(
                "artipie.storage.exists(TIMER)[id='InMemoryStorage']; count=1.0"
            )
        );
    }

    @Test
    void logsListOp() {
        this.asto.list(Key.ROOT).join();
        MatcherAssert.assertThat(
            this.registry.getMetersAsString(),
            Matchers.stringContainsInOrder(
                "artipie.storage.list(TIMER)[id='InMemoryStorage']; count=1.0"
            )
        );
    }

    @Test
    void logsSaveAndMoveOps() {
        this.asto.save(
            MicrometerStorageTest.KEY, new Content.From("abc".getBytes(StandardCharsets.UTF_8))
        ).join();
        MatcherAssert.assertThat(
            "Logged save",
            Arrays.stream(this.registry.getMetersAsString().split("\n")).toList(),
            Matchers.containsInAnyOrder(
                Matchers.containsString("artipie.storage.save(TIMER)[id='InMemoryStorage']; count=1.0"),
                Matchers.containsString("artipie.storage.save.size(DISTRIBUTION_SUMMARY)[id='InMemoryStorage']; count=1.0, total=3.0 bytes, max=3.0")
            )
        );
        this.asto.move(MicrometerStorageTest.KEY, new Key.From("other/location/test.txt")).join();
        MatcherAssert.assertThat(
            "Logged save and move",
            Arrays.stream(this.registry.getMetersAsString().split("\n")).toList(),
            Matchers.containsInAnyOrder(
                Matchers.containsString("artipie.storage.save(TIMER)[id='InMemoryStorage']; count=1.0"),
                Matchers.containsString("artipie.storage.save.size(DISTRIBUTION_SUMMARY)[id='InMemoryStorage']; count=1.0, total=3.0 bytes, max=3.0"),
                Matchers.containsString("artipie.storage.move(TIMER)[id='InMemoryStorage']; count=1.0")
            )
        );
    }

    @Test
    void logsSaveAndValueOps() {
        this.asto.save(
            MicrometerStorageTest.KEY, new Content.From("123".getBytes(StandardCharsets.UTF_8))
        ).join();
        MatcherAssert.assertThat(
            "Logged save",
            Arrays.stream(this.registry.getMetersAsString().split("\n")).toList(),
            Matchers.containsInAnyOrder(
                Matchers.containsString("artipie.storage.save(TIMER)[id='InMemoryStorage']; count=1.0"),
                Matchers.containsString("artipie.storage.save.size(DISTRIBUTION_SUMMARY)[id='InMemoryStorage']; count=1.0, total=3.0 bytes, max=3.0")
            )
        );
        this.asto.value(MicrometerStorageTest.KEY).thenAccept(
            content -> Flowable.fromPublisher(content).blockingSubscribe()
        ).join();
        MatcherAssert.assertThat(
            "Logged save and value",
            Arrays.stream(this.registry.getMetersAsString().split("\n")).toList(),
            Matchers.containsInAnyOrder(
                Matchers.containsString("artipie.storage.save(TIMER)[id='InMemoryStorage']; count=1.0"),
                Matchers.containsString("artipie.storage.value(TIMER)[id='InMemoryStorage']; count=1.0"),
                Matchers.containsString("artipie.storage.save.size(DISTRIBUTION_SUMMARY)[id='InMemoryStorage']; count=1.0, total=3.0 bytes, max=3.0"),
                Matchers.containsString("artipie.storage.value.size(DISTRIBUTION_SUMMARY)[id='InMemoryStorage']; count=1.0, total=3.0 bytes, max=3.0")
            )
        );
    }

    @Test
    void logsSaveMetaAndDeleteOps() {
        this.asto.save(
            MicrometerStorageTest.KEY, new Content.From("xyz".getBytes(StandardCharsets.UTF_8))
        ).join();
        this.asto.metadata(MicrometerStorageTest.KEY).join();
        this.asto.delete(MicrometerStorageTest.KEY).join();
        MatcherAssert.assertThat(
            Arrays.stream(this.registry.getMetersAsString().split("\n")).toList(),
            Matchers.containsInAnyOrder(
                Matchers.containsString("artipie.storage.save(TIMER)[id='InMemoryStorage']; count=1.0"),
                Matchers.containsString("artipie.storage.delete(TIMER)[id='InMemoryStorage']; count=1.0"),
                Matchers.containsString("artipie.storage.metadata(TIMER)[id='InMemoryStorage']; count=1.0"),
                Matchers.containsString("artipie.storage.save.size(DISTRIBUTION_SUMMARY)[id='InMemoryStorage']; count=1.0, total=3.0 bytes, max=3.0")
            )
        );
    }

    @Test
    void logsSaveExclusivelyAndDeleteAll() {
        this.asto.save(
            MicrometerStorageTest.KEY, new Content.From("xyz".getBytes(StandardCharsets.UTF_8))
        ).join();
        this.asto.exclusively(
            MicrometerStorageTest.KEY, storage -> CompletableFuture.completedFuture("ignored")
        ).toCompletableFuture().join();
        this.asto.deleteAll(MicrometerStorageTest.KEY).join();
        MatcherAssert.assertThat(
            Arrays.stream(this.registry.getMetersAsString().split("\n")).toList(),
            Matchers.containsInAnyOrder(
                Matchers.containsString("artipie.storage.save(TIMER)[id='InMemoryStorage']; count=1.0"),
                Matchers.containsString("artipie.storage.exclusively(TIMER)[id='InMemoryStorage']; count=1.0"),
                Matchers.containsString("artipie.storage.deleteAll(TIMER)[id='InMemoryStorage']; count=1.0"),
                Matchers.containsString("artipie.storage.save.size(DISTRIBUTION_SUMMARY)[id='InMemoryStorage']; count=1.0, total=3.0 bytes, max=3.0")
            )
        );
    }

    @Test
    void logsErrorAndCompletesWithException() {
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                CompletionException.class,
                () -> this.asto.value(MicrometerStorageTest.KEY).join()
            ).getCause(),
            new IsInstanceOf(ValueNotFoundException.class)
        );
        MatcherAssert.assertThat(
            this.registry.getMetersAsString(),
            Matchers.containsString(
                "artipie.storage.value.error(TIMER)[id='InMemoryStorage']; count=1.0"
            )
        );
    }

}

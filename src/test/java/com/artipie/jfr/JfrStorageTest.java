/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.jfr;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Splitting;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import io.reactivex.Flowable;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingStream;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests JFR storage events.
 *
 * @since 0.28.0
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle LocalFinalVariableNameCheck (500 lines)
 */
@SuppressWarnings(
    {
        "PMD.AvoidDuplicateLiterals",
        "PMD.ProhibitPlainJunitAssertionsRule",
        "PMD.TooManyMethods"
    }
)
class JfrStorageTest {
    /**
     * Random one for all tests.
     */
    private static final Random RANDOM = new Random();

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new JfrStorage(new InMemoryStorage());
    }

    @Test
    void shouldPublishStorageSaveEventWhenSave() {
        final int size = 10 * 1024;
        final int chunks = 5;
        final Key key = new Key.From("test-key");
        final RecordedEvent event = process(
            "artipie.StorageSave",
            () -> this.storage.save(key, content(size, chunks))
        );
        assertEvent(event, key.string());
        MatcherAssert.assertThat(event.getInt("chunks"), Is.is(chunks));
        MatcherAssert.assertThat(event.getLong("size"), Is.is((long) size));
    }

    @Test
    void shouldPublishStorageValueEventWhenValue() {
        final int size = 4 * 1024;
        final Key key = new Key.From("test-key");
        this.storage.save(key, content(size, 1));
        final RecordedEvent event = process(
            "artipie.StorageValue",
            () -> Flowable.fromPublisher(this.storage.value(key).join())
                .blockingSubscribe()
        );
        assertEvent(event, key.string());
        MatcherAssert.assertThat(event.getInt("chunks"), Is.is(1));
        MatcherAssert.assertThat(event.getLong("size"), Is.is((long) size));
    }

    @Test
    void shouldPublishStorageDeleteEventWhenDelete() {
        final Key key = new Key.From("test-key");
        this.storage.save(key, content(1024, 1));
        final RecordedEvent event = process(
            "artipie.StorageDelete",
            () -> this.storage.delete(key)
        );
        assertEvent(event, key.string());
    }

    @Test
    void shouldPublishStorageDeleteAllEventWhenDeleteAll() {
        final Key base = new Key.From("test");
        this.storage.save(new Key.From(base, "1"), content(1024, 1));
        this.storage.save(new Key.From(base, "2"), content(1024, 1));
        this.storage.save(new Key.From(base, "3"), content(1024, 1));
        final RecordedEvent event = process(
            "artipie.StorageDeleteAll",
            () -> this.storage.deleteAll(base)
        );
        assertEvent(event, base.string());
    }

    @Test
    void shouldPublishStorageExclusivelyEventWhenExclusively() {
        final Key key = new Key.From("test-Exclusively");
        this.storage.save(key, content(1024, 2));
        final RecordedEvent event = process(
            "artipie.StorageExclusively",
            () -> this.storage.exclusively(
                key,
                stor -> CompletableFuture.allOf()
            )
        );
        assertEvent(event, key.string());
    }

    @Test
    void shouldPublishStorageMoveEventWhenMove() {
        final Key key = new Key.From("test-key");
        final Key target = new Key.From("new-test-key");
        this.storage.save(key, content(1024, 2));
        final RecordedEvent event = process(
            "artipie.StorageMove",
            () -> this.storage.move(key, target)
        );
        assertEvent(event, key.string());
        MatcherAssert.assertThat(event.getString("target"), Is.is(target.string()));
    }

    @Test
    void shouldPublishStorageListEventWhenList() {
        final Key base = new Key.From("test");
        this.storage.save(new Key.From(base, "1"), content(1024, 1));
        this.storage.save(new Key.From(base, "2"), content(1024, 1));
        this.storage.save(new Key.From(base, "3"), content(1024, 1));
        final RecordedEvent event = process(
            "artipie.StorageList",
            () -> this.storage.list(base)
        );
        assertEvent(event, base.string());
        MatcherAssert.assertThat(event.getInt("keysCount"), Is.is(3));
    }

    /**
     * Asserts common event fields.
     *
     * @param event Recorded event.
     * @param key Key to check.
     */
    private static void assertEvent(final RecordedEvent event, final String key) {
        MatcherAssert.assertThat(
            event.getString("storage"),
            Is.is("InMemoryStorage")
        );
        MatcherAssert.assertThat(event.getString("key"), Is.is(key));
    }

    /**
     * Processes action to get according event.
     *
     * @param event Event name.
     * @param action Action that triggers event.
     * @return Recorded event.
     */
    private static RecordedEvent process(final String event,
        final Runnable action) {
        try (RecordingStream rs = new RecordingStream()) {
            final AtomicReference<RecordedEvent> ref = new AtomicReference<>();
            rs.onEvent(event, ref::set);
            rs.startAsync();
            action.run();
            Awaitility.waitAtMost(3_000, TimeUnit.MILLISECONDS)
                .until(() -> ref.get() != null);
            return ref.get();
        }
    }

    /**
     * Creates content.
     *
     * @param size Size of content's data.
     * @param chunks Chunks count.
     * @return Content.
     */
    private static Content content(final int size, final int chunks) {
        final byte[] data = new byte[size];
        RANDOM.nextBytes(data);
        final int rest = size % chunks;
        final int chunkSize = size / chunks + rest;
        return new Content.From(
            Flowable.fromPublisher(new Content.From(data))
                .flatMap(
                    buffer -> new Splitting(
                        buffer,
                        chunkSize
                    ).publisher()
                ));
    }
}

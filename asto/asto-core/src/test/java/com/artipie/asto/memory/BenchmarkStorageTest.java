/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.memory;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.ValueNotFoundException;
import com.artipie.asto.ext.PublisherAs;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CompletionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link BenchmarkStorage}.
 * @since 1.1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class BenchmarkStorageTest {
    @Test
    void obtainsValueFromBackendIfAbsenceInLocal() {
        final Key key = new Key.From("somekey");
        final byte[] data = "some data".getBytes();
        final NavigableMap<String, byte[]> backdata = new TreeMap<>();
        backdata.put(key.string(), data);
        final InMemoryStorage memory = new InMemoryStorage(backdata);
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        MatcherAssert.assertThat(
            this.valueFrom(bench, key),
            new IsEqual<>(data)
        );
    }

    @Test
    void obtainsValueFromLocalWithEmptyBackend() {
        final Key key = new Key.From("somekey");
        final byte[] data = "some data".getBytes();
        final BenchmarkStorage bench = new BenchmarkStorage(new InMemoryStorage());
        bench.save(key, new Content.From(data)).join();
        bench.save(new Key.From("another"), Content.EMPTY).join();
        MatcherAssert.assertThat(
            this.valueFrom(bench, key),
            new IsEqual<>(data)
        );
    }

    @Test
    void obtainsValueFromLocalWhenInLocalAndBackedIsPresent() {
        final Key key = new Key.From("somekey");
        final byte[] lcl = "some local data".getBytes();
        final byte[] back = "some backend data".getBytes();
        final NavigableMap<String, byte[]> backdata = new TreeMap<>();
        backdata.put(key.string(), back);
        final BenchmarkStorage bench = new BenchmarkStorage(new InMemoryStorage(backdata));
        bench.save(key, new Content.From(lcl)).join();
        MatcherAssert.assertThat(
            this.valueFrom(bench, key),
            new IsEqual<>(lcl)
        );
    }

    @Test
    void savesOnlyInLocal() {
        final InMemoryStorage memory = new InMemoryStorage();
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        final Key key = new Key.From("somekey");
        final byte[] data = "should save in local".getBytes();
        bench.save(key, new Content.From(data)).join();
        MatcherAssert.assertThat(
            "Value was not saved in local storage",
            this.valueFrom(bench, key),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Value was saved in backend storage",
            memory.exists(key).join(),
            new IsEqual<>(false)
        );
    }

    @Test
    void returnsNotFoundIfValueWasDeleted() {
        final InMemoryStorage memory = new InMemoryStorage();
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        final Key key = new Key.From("somekey");
        bench.save(key, new Content.From("any data".getBytes())).join();
        bench.delete(key);
        final Throwable thr = Assertions.assertThrows(
            CompletionException.class,
            () -> bench.value(key).join()
        );
        MatcherAssert.assertThat(
            thr.getCause(),
            new IsInstanceOf(ValueNotFoundException.class)
        );
    }

    private byte[] valueFrom(final BenchmarkStorage bench, final Key key) {
        return new PublisherAs(bench.value(key).join())
            .bytes()
            .toCompletableFuture().join();
    }
}

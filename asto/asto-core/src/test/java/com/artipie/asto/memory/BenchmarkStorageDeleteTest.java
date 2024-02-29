/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.memory;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.ValueNotFoundException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.CompletionException;

/**
 * Tests for {@link BenchmarkStorage#delete(Key)}.
 */
final class BenchmarkStorageDeleteTest {
    @Test
    void obtainsValueWhichWasAddedBySameKeyAfterDeletionToVerifyDeletedWasReset() {
        final InMemoryStorage memory = new InMemoryStorage();
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        final Key key = new Key.From("somekey");
        bench.save(key, new Content.From("old data".getBytes())).join();
        bench.delete(key).join();
        final byte[] upd = "updated data".getBytes();
        bench.save(key, new Content.From(upd)).join();
        Assertions.assertArrayEquals(upd, bench.value(key).join().asBytes());
    }

    @Test
    void returnsNotFoundIfValueWasDeletedButPresentInBackend() {
        final Key key = new Key.From("somekey");
        final NavigableMap<String, byte[]> backdata = new TreeMap<>();
        backdata.put(key.string(), "shouldBeObtained".getBytes());
        final InMemoryStorage memory = new InMemoryStorage(backdata);
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        bench.delete(key).join();
        final Throwable thr = Assertions.assertThrows(
            CompletionException.class,
            () -> bench.value(key).join()
        );
        MatcherAssert.assertThat(
            thr.getCause(),
            new IsInstanceOf(ValueNotFoundException.class)
        );
    }
}

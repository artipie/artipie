/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.memory;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
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
 * Tests for {@link BenchmarkStorage#move(Key, Key)}.
 * @since 1.2.0
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class BenchmarkStorageMoveTest {
    @Test
    void movesWhenPresentInLocalAndNotDeleted() {
        final InMemoryStorage memory = new InMemoryStorage();
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        final byte[] data = "saved data".getBytes();
        final Key src = new Key.From("someLocalkey");
        final Key dest = new Key.From("destination");
        bench.save(src, new Content.From(data)).join();
        bench.move(src, dest).join();
        MatcherAssert.assertThat(
            "Value was not moved to destination key",
            new PublisherAs(bench.value(dest).join()).bytes().toCompletableFuture().join(),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Source key in local was not removed",
            bench.exists(src).join(),
            new IsEqual<>(false)
        );
    }

    @Test
    void movesWhenPresentInLocalAndNotDeletedButDestinationIsDeleted() {
        final InMemoryStorage memory = new InMemoryStorage();
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        final byte[] data = "saved data".getBytes();
        final Key src = new Key.From("someLocalkey");
        final Key dest = new Key.From("destination");
        bench.save(src, new Content.From(data)).join();
        bench.save(dest, Content.EMPTY).join();
        bench.delete(dest).join();
        bench.move(src, dest).join();
        MatcherAssert.assertThat(
            "Value was not moved to destination key",
            new PublisherAs(bench.value(dest).join()).bytes().toCompletableFuture().join(),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Source key in local was not removed",
            bench.exists(src).join(),
            new IsEqual<>(false)
        );
    }

    @Test
    void movesWhenPresentInBackendAndNotDeleted() {
        final Key src = new Key.From("someBackendkey");
        final Key dest = new Key.From("destinationInLocal");
        final byte[] data = "saved data".getBytes();
        final NavigableMap<String, byte[]> backdata = new TreeMap<>();
        backdata.put(src.string(), data);
        final InMemoryStorage memory = new InMemoryStorage(backdata);
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        bench.move(src, dest).join();
        MatcherAssert.assertThat(
            "Value was not moved to destination key",
            new PublisherAs(bench.value(dest).join()).bytes().toCompletableFuture().join(),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Source key in backend storage should not be touched",
            bench.exists(src).join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void movesWhenPresentInBackendAndNotDeletedButDestinationIsDeleted() {
        final Key src = new Key.From("someBackendkey");
        final Key dest = new Key.From("destinationInLocal");
        final byte[] data = "saved data".getBytes();
        final NavigableMap<String, byte[]> backdata = new TreeMap<>();
        backdata.put(src.string(), data);
        backdata.put(dest.string(), "".getBytes());
        final InMemoryStorage memory = new InMemoryStorage(backdata);
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        bench.delete(dest).join();
        bench.move(src, dest).join();
        MatcherAssert.assertThat(
            "Value was not moved to destination key",
            new PublisherAs(bench.value(dest).join()).bytes().toCompletableFuture().join(),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Source key in backend storage should not be touched",
            bench.exists(src).join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void notConsiderDeletedKey() {
        final Key src = new Key.From("willBeDeleted");
        final Key dest = new Key.From("destinationKey");
        final NavigableMap<String, byte[]> backdata = new TreeMap<>();
        backdata.put(src.string(), "will be deleted".getBytes());
        final InMemoryStorage memory = new InMemoryStorage(backdata);
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        bench.delete(src).join();
        final Throwable thr = Assertions.assertThrows(
            CompletionException.class,
            () -> bench.move(src, dest).join()
        );
        MatcherAssert.assertThat(
            thr.getCause(),
            new IsInstanceOf(ArtipieIOException.class)
        );
    }
}

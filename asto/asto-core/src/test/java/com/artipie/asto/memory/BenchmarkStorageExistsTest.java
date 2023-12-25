/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.memory;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BenchmarkStorage#exists(Key)}.
 * @since 1.2.0
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class BenchmarkStorageExistsTest {
    @Test
    void existsWhenPresentInLocalAndNotDeleted() {
        final InMemoryStorage memory = new InMemoryStorage();
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        final Key key = new Key.From("somekey");
        bench.save(key, Content.EMPTY).join();
        MatcherAssert.assertThat(
            bench.exists(key).join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void existsWhenPresentInBackendAndNotDeleted() {
        final Key key = new Key.From("somekey");
        final NavigableMap<String, byte[]> backdata = new TreeMap<>();
        backdata.put(key.string(), "shouldExist".getBytes());
        final InMemoryStorage memory = new InMemoryStorage(backdata);
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        MatcherAssert.assertThat(
            bench.exists(key).join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void notExistsIfKeyWasDeleted() {
        final InMemoryStorage memory = new InMemoryStorage();
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        final Key key = new Key.From("somekey");
        bench.save(key, new Content.From("any data".getBytes())).join();
        bench.delete(key).join();
        MatcherAssert.assertThat(
            bench.exists(key).join(),
            new IsEqual<>(false)
        );
    }
}

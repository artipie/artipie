/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.memory;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BenchmarkStorage#list(Key)}.
 * @since 1.2.0
 */
final class BenchmarkStorageListTest {
    @Test
    void returnsListWhenPresentInLocalAndNotDeleted() {
        final InMemoryStorage memory = new InMemoryStorage();
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        final Key key = new Key.From("someLocalkey");
        bench.save(key, Content.EMPTY).join();
        MatcherAssert.assertThat(
            bench.list(key).join(),
            new IsEqual<>(Collections.singleton(key))
        );
    }

    @Test
    void returnsListWhenPresentInBackendAndNotDeleted() {
        final Key key = new Key.From("someBackendkey");
        final NavigableMap<String, byte[]> backdata = new TreeMap<>();
        backdata.put(key.string(), "".getBytes());
        final InMemoryStorage memory = new InMemoryStorage(backdata);
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        MatcherAssert.assertThat(
            bench.list(key).join(),
            new IsEqual<>(Collections.singleton(key))
        );
    }

    @Test
    void returnListWhenSomeOfKeysWereDeleted() {
        final byte[] data = "saved data".getBytes();
        final Key prefix = new Key.From("somePrefix");
        final Key keyone = new Key.From(prefix, "0", "someBackendKey");
        final Key keytwo = new Key.From(prefix, "2", "orderImportant");
        final Key keydel = new Key.From(prefix, "1", "shouldBeDeleted");
        final NavigableMap<String, byte[]> backdata = new TreeMap<>();
        backdata.put(keydel.string(), data);
        backdata.put(keyone.string(), data);
        backdata.put(keytwo.string(), data);
        final InMemoryStorage memory = new InMemoryStorage(backdata);
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        bench.delete(keydel).join();
        MatcherAssert.assertThat(
            bench.list(prefix).join(),
            Matchers.containsInAnyOrder(keyone, keytwo)
        );
    }

    @Test
    void combineKeysFromLocalAndBackendStorages() {
        final Key prfx = new Key.From("prefix");
        final Key bcknd = new Key.From(prfx, "backendkey");
        final NavigableMap<String, byte[]> backdata = new TreeMap<>();
        backdata.put(bcknd.string(), "".getBytes());
        final InMemoryStorage memory = new InMemoryStorage(backdata);
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        final Key lcl = new Key.From(prfx, "localkey");
        bench.save(lcl, Content.EMPTY).join();
        MatcherAssert.assertThat(
            bench.list(prfx).join(),
            Matchers.containsInAnyOrder(bcknd, lcl)
        );
    }

    @Test
    void notConsiderDeletedKey() {
        final Key delkey = new Key.From("willBeDeleted");
        final Key existkey = new Key.From("shouldRemain");
        final NavigableMap<String, byte[]> backdata = new TreeMap<>();
        backdata.put(delkey.string(), "will be deleted".getBytes());
        backdata.put(existkey.string(), "should remain".getBytes());
        final InMemoryStorage memory = new InMemoryStorage(backdata);
        final BenchmarkStorage bench = new BenchmarkStorage(memory);
        bench.delete(delkey).join();
        MatcherAssert.assertThat(
            bench.list(Key.ROOT).join(),
            new IsEqual<>(Collections.singleton(existkey))
        );
    }
}

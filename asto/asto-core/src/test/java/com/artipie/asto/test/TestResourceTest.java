/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.test;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link TestResource}.
 * @since 0.24
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class TestResourceTest {

    @Test
    void readsResourceBytes() {
        MatcherAssert.assertThat(
            new TestResource("test.txt").asBytes(),
            new IsEqual<>("hello world".getBytes())
        );
    }

    @Test
    void readsResourceAsStream() {
        MatcherAssert.assertThat(
            new TestResource("test.txt").asInputStream(),
            new IsNot<>(new IsNull<>())
        );
    }

    @Test
    void addsToStorage() {
        final Storage storage = new InMemoryStorage();
        final String path = "test.txt";
        new TestResource(path).saveTo(storage);
        MatcherAssert.assertThat(
            new PublisherAs(storage.value(new Key.From(path)).join())
                .bytes().toCompletableFuture().join(),
            new IsEqual<>("hello world".getBytes())
        );
    }

    @Test
    void saveToPath(@TempDir final Path tmp) throws Exception {
        final Path target = tmp.resolve("target");
        new TestResource("test.txt").saveTo(target);
        MatcherAssert.assertThat(
            Files.readAllLines(target),
            Matchers.contains("hello world")
        );
    }

    @Test
    void addsToStorageBySpecifiedKey() {
        final Storage storage = new InMemoryStorage();
        final Key key = new Key.From("one");
        new TestResource("test.txt").saveTo(storage, key);
        MatcherAssert.assertThat(
            new PublisherAs(storage.value(key).join()).bytes().toCompletableFuture().join(),
            new IsEqual<>("hello world".getBytes())
        );
    }

    @Test
    void addsFilesToStorage() {
        final Storage storage = new InMemoryStorage();
        final Key base = new Key.From("base");
        new TestResource("folder").addFilesTo(storage, base);
        MatcherAssert.assertThat(
            "Adds one.txt",
            new PublisherAs(storage.value(new Key.From(base, "one.txt")).join())
                .bytes().toCompletableFuture().join(),
            new IsEqual<>("one".getBytes())
        );
        MatcherAssert.assertThat(
            "Adds two.txt",
            new PublisherAs(storage.value(new Key.From(base, "two.txt")).join())
                .bytes().toCompletableFuture().join(),
            new IsEqual<>("two".getBytes())
        );
    }

}

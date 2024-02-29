/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.test;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test for {@link TestResource}.
 */
class TestResourceTest {

    @Test
    void readsResourceBytes() {
        Assertions.assertArrayEquals(
            "hello world".getBytes(),
            new TestResource("test.txt").asBytes()
        );
    }

    @Test
    void readsResourceAsStream() {
        Assertions.assertNotNull(new TestResource("test.txt").asInputStream());
    }

    @Test
    void addsToStorage() {
        final Storage storage = new InMemoryStorage();
        final String path = "test.txt";
        new TestResource(path).saveTo(storage);
        Assertions.assertEquals("hello world",
            storage.value(new Key.From(path)).join().asString());
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
        Assertions.assertArrayEquals(
            "hello world".getBytes(),
            storage.value(key).join().asBytes()
        );
    }

    @Test
    void addsFilesToStorage() {
        final Storage storage = new InMemoryStorage();
        final Key base = new Key.From("base");
        new TestResource("folder").addFilesTo(storage, base);
        Assertions.assertEquals("one", storage.value(new Key.From(base, "one.txt")).join().asString());
        Assertions.assertEquals("two", storage.value(new Key.From(base, "two.txt")).join().asString());
    }

}

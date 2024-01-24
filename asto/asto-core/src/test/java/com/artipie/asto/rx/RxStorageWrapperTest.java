/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.rx;

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import io.reactivex.Single;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RxStorageWrapper}.
 *
 * @since 1.11
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class RxStorageWrapperTest {

    /**
     * Original storage.
     */
    private Storage original;

    /**
     * Reactive wrapper of original storage.
     */
    private RxStorageWrapper wrapper;

    @BeforeEach
    void setUp() {
        this.original = new InMemoryStorage();
        this.wrapper = new RxStorageWrapper(this.original);
    }

    @Test
    void checksExistence() {
        final Key key = new Key.From("a");
        this.original.save(key, Content.EMPTY).join();
        MatcherAssert.assertThat(
            this.wrapper.exists(key).blockingGet(),
            new IsEqual<>(true)
        );
    }

    @Test
    void listsItemsByPrefix() {
        this.original.save(new Key.From("a/b/c"), Content.EMPTY).join();
        this.original.save(new Key.From("a/d"), Content.EMPTY).join();
        this.original.save(new Key.From("z"), Content.EMPTY).join();
        final Collection<String> keys = this.wrapper.list(new Key.From("a"))
            .blockingGet()
            .stream()
            .map(Key::string)
            .collect(Collectors.toList());
        MatcherAssert.assertThat(
            keys,
            new IsEqual<>(Arrays.asList("a/b/c", "a/d"))
        );
    }

    @Test
    void savesItems() {
        this.wrapper.save(
            new Key.From("foo/file1"), Content.EMPTY
        ).blockingAwait();
        this.wrapper.save(
            new Key.From("file2"), Content.EMPTY
        ).blockingAwait();
        final Collection<String> keys = this.original.list(Key.ROOT)
            .join()
            .stream()
            .map(Key::string)
            .collect(Collectors.toList());
        MatcherAssert.assertThat(
            keys,
            new IsEqual<>(Arrays.asList("file2", "foo/file1"))
        );
    }

    @Test
    void movesItems() {
        final Key source = new Key.From("foo/file1");
        final Key destination = new Key.From("bla/file2");
        final byte[] bvalue = "my file1 content"
            .getBytes(StandardCharsets.UTF_8);
        this.original.save(
            source, new Content.From(bvalue)
        ).join();
        this.original.save(
            destination, Content.EMPTY
        ).join();
        this.wrapper.move(source, destination).blockingAwait();
        MatcherAssert.assertThat(
            new BlockingStorage(this.original)
                .value(destination),
            new IsEqual<>(bvalue)
        );
    }

    @Test
    @SuppressWarnings("deprecation")
    void readsSize() {
        final Key key = new Key.From("file.txt");
        final String text = "my file content";
        this.original.save(
            key,
            new Content.From(
                text.getBytes(StandardCharsets.UTF_8)
            )
        ).join();
        MatcherAssert.assertThat(
            this.wrapper.size(key).blockingGet(),
            new IsEqual<>((long) text.length())
        );
    }

    @Test
    void readsValue() {
        final Key key = new Key.From("a/z");
        final byte[] bvalue = "value to read"
            .getBytes(StandardCharsets.UTF_8);
        this.original.save(
            key, new Content.From(bvalue)
        ).join();
        MatcherAssert.assertThat(
            new Remaining(
                new Concatenation(
                    this.wrapper.value(key).blockingGet()
                ).single()
                .blockingGet(),
                true
            ).bytes(),
            new IsEqual<>(bvalue)
        );
    }

    @Test
    void deletesItem() throws Exception {
        final Key key = new Key.From("key_to_delete");
        this.original.save(key, Content.EMPTY).join();
        this.wrapper.delete(key).blockingAwait();
        MatcherAssert.assertThat(
            this.original.exists(key).get(),
            new IsEqual<>(false)
        );
    }

    @Test
    void runsExclusively() {
        final Key key = new Key.From("exclusively_key");
        final Function<RxStorage, Single<Integer>> operation = sto -> Single.just(1);
        this.wrapper.exclusively(key, operation).blockingGet();
        MatcherAssert.assertThat(
            this.wrapper.exclusively(key, operation).blockingGet(),
            new IsEqual<>(1)
        );
    }

}

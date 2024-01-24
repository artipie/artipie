/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link SubStorage}.
 * @since 1.9
 * @todo #352:30min Continue to add more tests for {@link SubStorage}.
 *  All the methods of the class should be verified, do not forget to
 *  add tests with different prefixes, including {@link Key#ROOT} as prefix.
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class SubStorageTest {

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void prefixedKeyEquals() {
        MatcherAssert.assertThat(
            Key.ROOT,
            Matchers.equalTo(new SubStorage.PrefixedKed(Key.ROOT, Key.ROOT))
        );
        MatcherAssert.assertThat(
            Key.ROOT,
            Matchers.not(
                Matchers.equalTo(new SubStorage.PrefixedKed(Key.ROOT, new Key.From("1")))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"pref", "composite/prefix"})
    void listsItems(final String pref) {
        final Key prefix = new Key.From(pref);
        this.asto.save(new Key.From(prefix, "one"), Content.EMPTY).join();
        this.asto.save(new Key.From(prefix, "one", "two"), Content.EMPTY).join();
        this.asto.save(new Key.From(prefix, "one", "two", "three"), Content.EMPTY).join();
        this.asto.save(new Key.From(prefix, "another"), Content.EMPTY).join();
        this.asto.save(new Key.From("no_prefix"), Content.EMPTY).join();
        MatcherAssert.assertThat(
            "Lists items with prefix by ROOT key",
            new SubStorage(prefix, this.asto).list(Key.ROOT).join(),
            Matchers.hasItems(
                new Key.From("one"),
                new Key.From("one/two"),
                new Key.From("one/two/three"),
                new Key.From("another")
            )
        );
        MatcherAssert.assertThat(
            "Lists item with prefix by `one/two` key",
            new SubStorage(prefix, this.asto).list(new Key.From("one/two")).join(),
            Matchers.hasItems(
                new Key.From("one/two"),
                new Key.From("one/two/three")
            )
        );
        MatcherAssert.assertThat(
            "Lists item with ROOT prefix by ROOT key",
            new SubStorage(Key.ROOT, this.asto).list(Key.ROOT).join(),
            new IsEqual<>(this.asto.list(Key.ROOT).join())
        );
        MatcherAssert.assertThat(
            "Lists item with ROOT prefix by `one` key",
            new SubStorage(Key.ROOT, this.asto).list(new Key.From("one")).join(),
            new IsEqual<>(this.asto.list(new Key.From("one")).join())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"my-project", "com/example"})
    void returnsValue(final String pref) {
        final Key prefix = new Key.From(pref);
        final byte[] data = "some data".getBytes(StandardCharsets.UTF_8);
        this.asto.save(new Key.From(prefix, "package"), new Content.From(data)).join();
        MatcherAssert.assertThat(
            "Returns storage item with prefix",
            new BlockingStorage(new SubStorage(prefix, this.asto)).value(new Key.From("package")),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Returns storage item with ROOT prefix",
            new BlockingStorage(new SubStorage(Key.ROOT, this.asto))
                .value(new Key.From(prefix, "package")),
            new IsEqual<>(data)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "sub/dir"})
    void checksExistence(final String pref) {
        final Key prefix = new Key.From(pref);
        this.asto.save(new Key.From(prefix, "any.txt"), Content.EMPTY).join();
        MatcherAssert.assertThat(
            "Returns true with prefix when item exists",
            new SubStorage(prefix, this.asto).exists(new Key.From("any.txt")).join()
        );
        MatcherAssert.assertThat(
            "Returns true with ROOT prefix when item exists",
            new SubStorage(Key.ROOT, this.asto).exists(new Key.From(prefix, "any.txt")).join()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"my-project", "com/example"})
    void savesContent(final String pref) {
        final Key prefix = new Key.From(pref);
        final byte[] data = "some data".getBytes(StandardCharsets.UTF_8);
        final SubStorage substo = new SubStorage(prefix, this.asto);
        substo.save(new Key.From("package"), new Content.From(data)).join();
        MatcherAssert.assertThat(
            "Returns storage item with prefix",
            new BlockingStorage(this.asto).value(new Key.From(prefix, "package")),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Returns storage item with ROOT prefix",
            new BlockingStorage(new SubStorage(Key.ROOT, this.asto))
                .value(new Key.From(prefix, "package")),
            new IsEqual<>(data)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"my-project", "com/example"})
    void movesContent(final String pref) {
        final Key prefix = new Key.From(pref);
        final byte[] data = "source".getBytes(StandardCharsets.UTF_8);
        final Key source = new Key.From("src");
        final Key destination = new Key.From("dest");
        this.asto.save(new Key.From(prefix, source), new Content.From(data)).join();
        this.asto.save(
            new Key.From(prefix, destination),
            new Content.From("destination".getBytes(StandardCharsets.UTF_8))
        ).join();
        final SubStorage substo = new SubStorage(prefix, this.asto);
        substo.move(source, destination).join();
        MatcherAssert.assertThat(
            "Moves key value with prefix",
            new BlockingStorage(this.asto).value(new Key.From(prefix, destination)),
            new IsEqual<>(data)
        );
        MatcherAssert.assertThat(
            "Moves key value with ROOT prefix",
            new BlockingStorage(new SubStorage(Key.ROOT, this.asto))
                .value(new Key.From(prefix, destination)),
            new IsEqual<>(data)
        );
    }

    @SuppressWarnings("deprecation")
    @ParameterizedTest
    @ValueSource(strings = {"url", "sub/url"})
    void readsSize(final String pref) {
        final Key prefix = new Key.From(pref);
        final byte[] data = "012004407".getBytes(StandardCharsets.UTF_8);
        final Long datalgt = (long) data.length;
        final Key keyres = new Key.From("resource");
        this.asto.save(new Key.From(prefix, keyres), new Content.From(data)).join();
        MatcherAssert.assertThat(
            "Gets value size with prefix",
            new BlockingStorage(new SubStorage(prefix, this.asto)).size(keyres),
            new IsEqual<>(datalgt)
        );
        MatcherAssert.assertThat(
            "Gets value size with ROOT prefix",
            new BlockingStorage(new SubStorage(Key.ROOT, this.asto))
                .size(new Key.From(prefix, keyres)),
            new IsEqual<>(datalgt)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"repo", "com/repo"})
    void deletesContent(final String pref) {
        final Key prefix = new Key.From(pref);
        final Key key = new Key.From("file");
        final Key prefkey = new Key.From(prefix, key);
        this.asto.save(prefkey, Content.EMPTY).join();
        new SubStorage(prefix, this.asto).delete(key).join();
        MatcherAssert.assertThat(
            "Deletes storage item with prefix",
            new BlockingStorage(this.asto).exists(prefkey),
            new IsEqual<>(false)
        );
        this.asto.save(prefkey, Content.EMPTY).join();
        new SubStorage(Key.ROOT, this.asto).delete(prefkey).join();
        MatcherAssert.assertThat(
            "Deletes storage item with ROOT prefix",
            new BlockingStorage(this.asto).exists(prefkey),
            new IsEqual<>(false)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"repo", "com/repo"})
    void readsMetadata(final String pref) {
        final Key prefix = new Key.From(pref);
        final Key key = new Key.From("file");
        final Key prefkey = new Key.From(prefix, key);
        final byte[] data = "My code is written here"
            .getBytes(StandardCharsets.UTF_8);
        final long dlg = data.length;
        this.asto.save(prefkey, new Content.From(data)).join();
        final Meta submeta =
            new SubStorage(prefix, this.asto).metadata(key).join();
        MatcherAssert.assertThat(
            "Reads storage metadata of a item with prefix",
            submeta.read(Meta.OP_SIZE).get(),
            new IsEqual<>(dlg)
        );
        final Meta rtmeta =
            new SubStorage(Key.ROOT, this.asto).metadata(prefkey).join();
        MatcherAssert.assertThat(
            "Reads storage metadata of a item with ROOT prefix",
            rtmeta.read(Meta.OP_SIZE).get(),
            new IsEqual<>(dlg)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"var", "var/repo"})
    void runsExclusively(final String pref) {
        final Key prefix = new Key.From(pref);
        final Key key = new Key.From("key-exec");
        final Key prefkey = new Key.From(prefix, key);
        this.asto.save(prefkey, Content.EMPTY).join();
        final Function<Storage, CompletionStage<Boolean>> operation =
            sto -> CompletableFuture.completedFuture(true);
        final Boolean subfinished = new LoggingStorage(this.asto)
            .exclusively(key, operation).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Runs exclusively a storage key with prefix",
            subfinished, new IsEqual<>(true)
        );
        final Boolean rtfinished = new SubStorage(Key.ROOT, this.asto)
            .exclusively(prefkey, operation).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Runs exclusively a storage key with ROOT prefix",
            rtfinished, new IsEqual<>(true)
        );
    }
}

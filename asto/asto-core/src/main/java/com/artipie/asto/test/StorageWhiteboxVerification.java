/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.test;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.ValueNotFoundException;
import com.artipie.asto.blocking.BlockingStorage;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Storage verification tests.
 * <p>
 * If a storage implementation passes this tests, it can be used like a storage in Artipie server.
 *
 * @since 1.14.0
 */
@SuppressWarnings({"deprecation", "PMD.MethodNamingConventions",
    "PMD.AvoidDuplicateLiterals", "PMD.AvoidCatchingGenericException",
    "PMD.TooManyMethods", "PMD.JUnit5TestShouldBePackagePrivate"})
@Disabled
public abstract class StorageWhiteboxVerification {

    @Test
    @Timeout(1)
    public void saveAndLoad_shouldSave() throws Exception {
        this.execute(
            pair -> {
                final BlockingStorage blocking = new BlockingStorage(pair.getValue());
                final byte[] data = "0".getBytes();
                final Key key = new Key.From("shouldSave");
                blocking.save(key, data);
                MatcherAssert.assertThat(
                    pair.getKey(),
                    blocking.value(key),
                    Matchers.equalTo(data)
                );
            }
        );
    }

    @Test
    @Timeout(1)
    public void saveAndLoad_shouldSaveFromMultipleBuffers() throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final Key key = new Key.From("shouldSaveFromMultipleBuffers");
                storage.save(
                    key,
                    new Content.OneTime(
                        new Content.From(
                            Flowable.fromArray(
                                ByteBuffer.wrap("12".getBytes()),
                                ByteBuffer.wrap("34".getBytes()),
                                ByteBuffer.wrap("5".getBytes())
                            )
                        )
                    )
                ).get();
                MatcherAssert.assertThat(
                    pair.getKey(),
                    new BlockingStorage(storage).value(key),
                    Matchers.equalTo("12345".getBytes())
                );
            }
        );
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    @Test
    @Timeout(1)
    public void saveAndLoad_shouldNotOverwriteWithPartial() throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final Key key = new Key.From("saveIsAtomic");
                final String initial = "initial";
                storage.save(
                    key,
                    new Content.OneTime(
                        new Content.From(Flowable.fromArray(ByteBuffer.wrap(initial.getBytes())))
                    )
                ).join();
                try {
                    storage.save(
                        key,
                        new Content.OneTime(
                            new Content.From(
                                Flowable.concat(
                                    Flowable.just(ByteBuffer.wrap(new byte[]{1})),
                                    Flowable.error(new IllegalStateException())
                                )
                            )
                        )
                    ).join();
                } catch (final Exception exc) {
                }
                MatcherAssert.assertThat(
                    String.format("%s: save should be atomic", pair.getKey()),
                    new String(new BlockingStorage(storage).value(key)),
                    Matchers.equalTo(initial)
                );
            }
        );
    }

    @Test
    @Timeout(1)
    public void saveAndLoad_shouldSaveEmpty() throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final Key key = new Key.From("shouldSaveEmpty");
                storage.save(key, new Content.OneTime(new Content.From(Flowable.empty()))).get();
                MatcherAssert.assertThat(
                    String.format("%s: saved content should be empty", pair.getKey()),
                    new BlockingStorage(storage).value(key),
                    Matchers.equalTo(new byte[0])
                );
            }
        );
    }

    @Test
    @Timeout(1)
    public void saveAndLoad_shouldSaveWhenValueAlreadyExists() throws Exception {
        this.execute(
            pair -> {
                final BlockingStorage blocking = new BlockingStorage(pair.getValue());
                final byte[] original = "1".getBytes();
                final byte[] updated = "2".getBytes();
                final Key key = new Key.From("shouldSaveWhenValueAlreadyExists");
                blocking.save(key, original);
                blocking.save(key, updated);
                MatcherAssert.assertThat(
                    pair.getKey(),
                    blocking.value(key),
                    Matchers.equalTo(updated)
                );
            }
        );
    }

    @Test
    @Timeout(1)
    public void saveAndLoad_shouldFailToSaveErrorContent() throws Exception {
        this.execute(
            pair -> Assertions.assertThrows(
                Exception.class,
                () -> pair.getValue().save(
                    new Key.From("shouldFailToSaveErrorContent"),
                    new Content.OneTime(
                        new Content.From(Flowable.error(new IllegalStateException()))
                    )
                ).join(),
                pair.getKey()
            )
        );
    }

    @Test
    @Timeout(1)
    public void saveAndLoad_shouldFailOnSecondConsumeAttempt() throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final Key.From key = new Key.From("key");
                storage.save(key, new Content.OneTime(new Content.From("val".getBytes()))).join();
                final Content value = storage.value(key).join();
                Flowable.fromPublisher(value).toList().blockingGet();
                Assertions.assertThrows(
                    ArtipieIOException.class,
                    () -> Flowable.fromPublisher(value).toList().blockingGet(),
                    pair.getKey()
                );
            }
        );
    }

    @Test
    @Timeout(1)
    public void saveAndLoad_shouldFailToLoadAbsentValue() throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final CompletableFuture<Content> value = storage.value(
                    new Key.From("shouldFailToLoadAbsentValue")
                );
                final Exception exception = Assertions.assertThrows(
                    CompletionException.class,
                    value::join
                );
                MatcherAssert.assertThat(
                    String.format(
                        "%s: storage '%s' should fail",
                        pair.getKey(), storage.getClass().getName()
                    ),
                    exception.getCause(),
                    new IsInstanceOf(ValueNotFoundException.class)
                );
            }
        );
    }

    @Test
    @Timeout(1)
    public void saveAndLoad_shouldNotSavePartial() throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final Key key = new Key.From("shouldNotSavePartial");
                storage.save(
                    key,
                    new Content.From(
                        Flowable.concat(
                            Flowable.just(ByteBuffer.wrap(new byte[]{1})),
                            Flowable.error(new IllegalStateException())
                        )
                    )
                ).exceptionally(ignored -> null).join();
                MatcherAssert.assertThat(
                    pair.getKey(),
                    storage.exists(key).join(),
                    Matchers.equalTo(false)
                );
            }
        );
    }

    @Test
    public void saveAndLoad_shouldReturnContentWithSpecifiedSize() throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final byte[] content = "1234".getBytes();
                final Key key = new Key.From("shouldReturnContentWithSpecifiedSize");
                storage.save(key, new Content.OneTime(new Content.From(content))).get();
                MatcherAssert.assertThat(
                    pair.getKey(),
                    storage.value(key).get().size().get(),
                    new IsEqual<>((long) content.length)
                );
            }
        );
    }

    @Test
    public void saveAndLoad_saveDoesNotSupportRootKey() throws Exception {
        this.execute(
            pair -> Assertions.assertThrows(
                ExecutionException.class,
                () -> pair.getValue().save(Key.ROOT, Content.EMPTY).get(),
                String.format(
                    "%s: `%s` storage didn't fail on root saving",
                    pair.getKey(), pair.getValue().getClass().getSimpleName()
                )
            )
        );
    }

    @Test
    public void saveAndLoad_loadDoesNotSupportRootKey() throws Exception {
        this.execute(
            pair -> Assertions.assertThrows(
                ExecutionException.class,
                () -> pair.getValue().value(Key.ROOT).get(),
                String.format(
                    "%s: `%s` storage didn't fail on root loading",
                    pair.getKey(), pair.getValue().getClass().getSimpleName()
                )
            )
        );
    }

    @Test
    public void exists_shouldExistForSavedKey() throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final Key key = new Key.From("shouldExistForSavedKey");
                final byte[] data = "some data".getBytes();
                new BlockingStorage(storage).save(key, data);
                MatcherAssert.assertThat(
                    pair.getKey(),
                    storage.exists(key).get(),
                    new IsEqual<>(true)
                );
            }
        );
    }

    @Test
    public void exists_shouldNotExistForUnknownKey() throws Exception {
        this.execute(
            pair -> {
                final Key key = new Key.From("shouldNotExistForUnknownKey");
                MatcherAssert.assertThat(
                    pair.getKey(),
                    pair.getValue().exists(key).get(),
                    new IsEqual<>(false)
                );
            }
        );
    }

    @Test
    public void exists_shouldNotExistForParentOfSavedKey() throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final Key parent = new Key.From("shouldNotExistForParentOfSavedKey");
                final Key key = new Key.From(parent, "child");
                final byte[] data = "content".getBytes();
                new BlockingStorage(storage).save(key, data);
                MatcherAssert.assertThat(
                    pair.getKey(),
                    storage.exists(parent).get(),
                    new IsEqual<>(false)
                );
            }
        );
    }

    @Test
    public void delete_shouldDeleteValue() throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final Key key = new Key.From("shouldDeleteValue");
                final byte[] data = "data".getBytes();
                final BlockingStorage blocking = new BlockingStorage(storage);
                blocking.save(key, data);
                blocking.delete(key);
                MatcherAssert.assertThat(
                    pair.getKey(),
                    storage.exists(key).get(),
                    new IsEqual<>(false)
                );
            }
        );
    }

    @Test
    public void delete_shouldFailToDeleteNotExistingValue() throws Exception {
        this.execute(
            pair -> {
                final Key key = new Key.From("shouldFailToDeleteNotExistingValue");
                Assertions.assertThrows(
                    Exception.class,
                    () -> pair.getValue().delete(key).get(),
                    pair.getKey()
                );
            }
        );
    }

    @Test
    public void delete_shouldFailToDeleteParentOfSavedKey() throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final Key parent = new Key.From("shouldFailToDeleteParentOfSavedKey");
                final Key key = new Key.From(parent, "child");
                final byte[] content = "content".getBytes();
                new BlockingStorage(storage).save(key, content);
                Assertions.assertThrows(
                    Exception.class,
                    () -> storage.delete(parent).get(),
                    pair.getKey()
                );
            }
        );
    }

    @Test
    public void deleteAll_shouldDeleteAllItemsWithKeyPrefix() throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final Key prefix = new Key.From("p1");
                storage.save(new Key.From(prefix, "one"), Content.EMPTY).join();
                storage.save(new Key.From(prefix, "two"), Content.EMPTY).join();
                storage.save(new Key.From("p2", "three"), Content.EMPTY).join();
                storage.save(new Key.From("four"), Content.EMPTY).join();
                final BlockingStorage blocking = new BlockingStorage(storage);
                blocking.deleteAll(prefix);
                MatcherAssert.assertThat(
                    String.format("%s: should not have items with key prefix", pair.getKey()),
                    blocking.list(prefix).size(),
                    new IsEqual<>(0)
                );
                MatcherAssert.assertThat(
                    String.format("%s: should list other items", pair.getKey()),
                    blocking.list(Key.ROOT),
                    Matchers.hasItems(
                        new Key.From("p2", "three"),
                        new Key.From("four")
                    )
                );
                blocking.deleteAll(Key.ROOT);
                MatcherAssert.assertThat(
                    String.format("%s: should remove all items", pair.getKey()),
                    blocking.list(Key.ROOT).size(),
                    new IsEqual<>(0)
                );
            }
        );
    }

    @Test
    public void exclusively_shouldFailExclusivelyForSameKey() throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final Key key = new Key.From("shouldFailConcurrentExclusivelyForSameKey");
                final FakeOperation operation = new FakeOperation();
                final CompletionStage<Void> exclusively = storage.exclusively(key, operation);
                operation.started.join();
                try {
                    final CompletionException completion = Assertions.assertThrows(
                        CompletionException.class,
                        () -> storage.exclusively(key, new FakeOperation())
                            .toCompletableFuture()
                            .join(),
                        pair.getKey()
                    );
                    MatcherAssert.assertThat(
                        pair.getKey(),
                        completion.getCause(),
                        new IsInstanceOf(ArtipieIOException.class)
                    );
                } finally {
                    operation.finished.complete(null);
                    exclusively.toCompletableFuture().join();
                }
            }
        );
    }

    @Test
    public void exclusively_shouldRunExclusivelyForDiffKey() throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final Key one = new Key.From("shouldRunExclusivelyForDiffKey-1");
                final Key two = new Key.From("shouldRunExclusivelyForDiffKey-2");
                final FakeOperation operation = new FakeOperation();
                final CompletionStage<Void> exclusively = storage.exclusively(one, operation);
                operation.started.join();
                try {
                    Assertions.assertDoesNotThrow(
                        () -> storage.exclusively(two, new FakeOperation(CompletableFuture.allOf()))
                            .toCompletableFuture()
                            .join(),
                        pair.getKey()
                    );
                } finally {
                    operation.finished.complete(null);
                    exclusively.toCompletableFuture().join();
                }
            }
        );
    }

    @Test
    public void exclusively_shouldRunExclusivelyWhenPrevFinished() throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final Key key = new Key.From("shouldRunExclusivelyWhenPrevFinished");
                final FakeOperation operation = new FakeOperation(CompletableFuture.allOf());
                storage.exclusively(key, operation).toCompletableFuture().join();
                Assertions.assertDoesNotThrow(
                    () -> storage
                        .exclusively(key, new FakeOperation(CompletableFuture.allOf()))
                        .toCompletableFuture()
                        .join(),
                    pair.getKey()
                );
            }
        );
    }

    @Test
    public void exclusively_shouldRunExclusivelyWhenPrevFinishedWithAsyncFailure()
        throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final Key key = new Key.From(
                    "shouldRunExclusivelyWhenPrevFinishedWithAsyncFailure"
                );
                final FakeOperation operation = new FakeOperation();
                operation.finished.completeExceptionally(new IllegalStateException());
                Assertions.assertThrows(
                    CompletionException.class,
                    () -> storage.exclusively(key, operation)
                        .toCompletableFuture().join(),
                    pair.getKey()
                );
                Assertions.assertDoesNotThrow(
                    () -> storage.exclusively(key, new FakeOperation(CompletableFuture.allOf()))
                        .toCompletableFuture().join(),
                    pair.getKey()
                );
            }
        );
    }

    @Test
    public void exclusively_shouldRunExclusivelyWhenPrevFinishedWithSyncFailure() throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final Key key = new Key.From("shouldRunExclusivelyWhenPrevFinishedWithSyncFailure");
                Assertions.assertThrows(
                    CompletionException.class,
                    () -> storage.exclusively(
                        key,
                        ignored -> {
                            throw new IllegalStateException();
                        }
                    ).toCompletableFuture().join(),
                    pair.getKey()
                );
                Assertions.assertDoesNotThrow(
                    () -> storage
                        .exclusively(key, new FakeOperation(CompletableFuture.allOf()))
                        .toCompletableFuture().join(),
                    pair.getKey()
                );
            }
        );
    }

    @Test
    public void list_shouldListNoKeysWhenEmpty() throws Exception {
        this.execute(
            pair -> {
                final BlockingStorage blocking = new BlockingStorage(pair.getValue());
                final Collection<String> keys = blocking.list(new Key.From("a", "b"))
                    .stream()
                    .map(Key::string)
                    .collect(Collectors.toList());
                MatcherAssert.assertThat(pair.getKey(), keys, Matchers.empty());
            }
        );
    }

    @Test
    public void list_shouldListAllItemsByRootKey() throws Exception {
        this.execute(
            pair -> {
                final BlockingStorage blocking = new BlockingStorage(pair.getValue());
                blocking.save(new Key.From("one", "file.txt"), new byte[]{});
                blocking.save(new Key.From("one", "two", "file.txt"), new byte[]{});
                blocking.save(new Key.From("another"), new byte[]{});
                final Collection<String> keys = blocking.list(Key.ROOT)
                    .stream()
                    .map(Key::string)
                    .collect(Collectors.toList());
                MatcherAssert.assertThat(
                    pair.getKey(),
                    keys,
                    Matchers.hasItems("one/file.txt", "one/two/file.txt", "another")
                );
            }
        );
    }

    @Test
    public void list_shouldListKeysInOrder() throws Exception {
        this.execute(
            pair -> {
                final byte[] data = "some data!".getBytes();
                final BlockingStorage blocking = new BlockingStorage(pair.getValue());
                blocking.save(new Key.From("1"), data);
                blocking.save(new Key.From("a", "b", "c", "1"), data);
                blocking.save(new Key.From("a", "b", "2"), data);
                blocking.save(new Key.From("a", "z"), data);
                blocking.save(new Key.From("z"), data);
                final Collection<String> keys = blocking.list(new Key.From("a", "b"))
                    .stream()
                    .map(Key::string)
                    .collect(Collectors.toList());
                MatcherAssert.assertThat(
                    pair.getKey(),
                    keys,
                    Matchers.equalTo(Arrays.asList("a/b/2", "a/b/c/1"))
                );
            }
        );
    }

    @Test
    @Timeout(2)
    public void move_shouldMove() throws Exception {
        this.execute(
            pair -> {
                final BlockingStorage blocking = new BlockingStorage(pair.getValue());
                final byte[] data = "source".getBytes();
                final Key source = new Key.From("shouldMove-source");
                final Key destination = new Key.From("shouldMove-destination");
                blocking.save(source, data);
                blocking.move(source, destination);
                MatcherAssert.assertThat(
                    pair.getKey(),
                    blocking.value(destination),
                    Matchers.equalTo(data)
                );
            }
        );
    }

    @Test
    @Timeout(2)
    public void move_shouldMoveWhenDestinationExists() throws Exception {
        this.execute(
            pair -> {
                final BlockingStorage blocking = new BlockingStorage(pair.getValue());
                final byte[] data = "source data".getBytes();
                final Key source = new Key.From("shouldMoveWhenDestinationExists-source");
                final Key destination = new Key.From("shouldMoveWhenDestinationExists-destination");
                blocking.save(source, data);
                blocking.save(destination, "destination data".getBytes());
                blocking.move(source, destination);
                MatcherAssert.assertThat(
                    pair.getKey(),
                    blocking.value(destination),
                    Matchers.equalTo(data)
                );
            }
        );
    }

    @Test
    @Timeout(2)
    public void move_shouldFailToMoveAbsentValue() throws Exception {
        this.execute(
            pair -> {
                final BlockingStorage blocking = new BlockingStorage(pair.getValue());
                final Key source = new Key.From("shouldFailToMoveAbsentValue-source");
                final Key destination = new Key.From("shouldFailToMoveAbsentValue-destination");
                Assertions.assertThrows(
                    RuntimeException.class,
                    () -> blocking.move(source, destination),
                    pair.getKey()
                );
            }
        );
    }

    @Test
    public void size_shouldGetSizeSave() throws Exception {
        this.execute(
            pair -> {
                final BlockingStorage blocking = new BlockingStorage(pair.getValue());
                final byte[] data = "0123456789".getBytes();
                final Key key = new Key.From("shouldGetSizeSave");
                blocking.save(key, data);
                MatcherAssert.assertThat(
                    pair.getKey(),
                    blocking.size(key),
                    new IsEqual<>((long) data.length)
                );
            }
        );
    }

    @Test
    public void size_shouldFailToGetSizeOfAbsentValue() throws Exception {
        this.execute(
            pair -> {
                final Storage storage = pair.getValue();
                final CompletableFuture<Long> size = storage.size(
                    new Key.From("shouldFailToGetSizeOfAbsentValue")
                );
                final Exception exception = Assertions.assertThrows(
                    CompletionException.class,
                    size::join
                );
                MatcherAssert.assertThat(
                    String.format(
                        "%s: storage '%s' should fail",
                        pair.getKey(), storage.getClass().getSimpleName()
                    ),
                    exception.getCause(),
                    new IsInstanceOf(ValueNotFoundException.class)
                );
            }
        );
    }

    /**
     * Creates a new instance of storage.
     *
     * @return Instance of storage.
     * @throws Exception If failed.
     */
    protected abstract Storage newStorage() throws Exception;

    /**
     * Creates a new instance of storage as a base storage for {@link SubStorage} with
     * {@link Key#ROOT} prefix.
     *
     * @return Instance of storage.
     * @throws Exception If failed.
     */
    protected Optional<Storage> newBaseForRootSubStorage() throws Exception {
        return Optional.of(this.newStorage());
    }

    /**
     * Creates a new instance of storage as a base storage for {@link SubStorage} with
     * custom prefix different from {@link Key#ROOT}.
     *
     * @return Instance of storage.
     * @throws Exception If failed.
     */
    protected Optional<Storage> newBaseForSubStorage() throws Exception {
        return Optional.of(this.newStorage());
    }

    private Stream<ImmutablePair<String, Storage>> storages() throws Exception {
        final List<ImmutablePair<String, Storage>> res = new ArrayList<>(3);
        res.add(ImmutablePair.of("Original storage", this.newStorage()));
        this.newBaseForRootSubStorage()
            .ifPresent(
                storage -> res.add(
                    ImmutablePair.of(
                        "Root sub storage",
                        new SubStorage(Key.ROOT, storage)
                    )
                )
            );
        this.newBaseForSubStorage()
            .ifPresent(
                storage -> res.add(
                    ImmutablePair.of(
                        "Sub storage with prefix",
                        new SubStorage(new Key.From("test-prefix"), storage)
                    )
                )
            );
        return res.stream();
    }

    private void execute(final StorageConsumer consumer) throws Exception {
        this.storages().forEach(
            ns -> {
                try {
                    consumer.accept(ns);
                } catch (final Exception err) {
                    throw new AssertionError(err);
                }
            }
        );
    }

    /**
     * Consumer that can throw {@code Exception}.
     *
     * @since 1.14.0
     */
    @FunctionalInterface
    interface StorageConsumer {
        void accept(ImmutablePair<String, Storage> pair) throws Exception;
    }

    /**
     * Fake operation with controllable start and finish.
     * Started future is completed when operation is invoked.
     * It could be used to await operation invocation.
     * Finished future is returned as result of operation.
     * It could be completed in order to finish operation.
     *
     * @since 0.27
     */
    private static final class FakeOperation implements Function<Storage, CompletionStage<Void>> {

        /**
         * Operation started future.
         */
        private final CompletableFuture<Void> started;

        /**
         * Operation finished future.
         */
        private final CompletableFuture<Void> finished;

        private FakeOperation() {
            this(new CompletableFuture<>());
        }

        private FakeOperation(final CompletableFuture<Void> finished) {
            this.started = new CompletableFuture<>();
            this.finished = finished;
        }

        @Override
        public CompletionStage<Void> apply(final Storage storage) {
            this.started.complete(null);
            return this.finished;
        }
    }

}

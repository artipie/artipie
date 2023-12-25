/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.lock.storage;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link StorageLock}.
 *
 * @since 0.24
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@Timeout(1)
final class StorageLockTest {

    /**
     * Storage used in tests.
     */
    private final InMemoryStorage storage = new InMemoryStorage();

    /**
     * Lock target key.
     */
    private final Key target = new Key.From("a/b/c");

    @Test
    void shouldAddEmptyValueWhenAcquiredLock() throws Exception {
        final String uuid = UUID.randomUUID().toString();
        new StorageLock(this.storage, this.target, uuid, Optional.empty())
            .acquire()
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            new BlockingStorage(this.storage).value(
                new Key.From(new Proposals.RootKey(this.target), uuid)
            ),
            new IsEqual<>(new byte[]{})
        );
    }

    @Test
    void shouldAddDateValueWhenAcquiredLock() throws Exception {
        final String uuid = UUID.randomUUID().toString();
        final String time = "2020-08-18T13:09:30.429Z";
        new StorageLock(this.storage, this.target, uuid, Optional.of(Instant.parse(time)))
            .acquire()
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            new BlockingStorage(this.storage).value(
                new Key.From(new Proposals.RootKey(this.target), uuid)
            ),
            new IsEqual<>(time.getBytes())
        );
    }

    @Test
    void shouldAcquireWhenValuePresents() {
        final String uuid = UUID.randomUUID().toString();
        this.storage.save(
            new Key.From(new Proposals.RootKey(this.target), uuid),
            Content.EMPTY
        ).toCompletableFuture().join();
        final StorageLock lock = new StorageLock(this.storage, this.target, uuid, Optional.empty());
        Assertions.assertDoesNotThrow(() -> lock.acquire().toCompletableFuture().join());
    }

    @Test
    void shouldAcquireWhenOtherProposalIsDeletedConcurrently() {
        final StorageLock lock = new StorageLock(
            new PhantomKeyStorage(
                this.storage,
                new Key.From(new Proposals.RootKey(this.target), UUID.randomUUID().toString())
            ),
            this.target,
            UUID.randomUUID().toString(),
            Optional.empty()
        );
        Assertions.assertDoesNotThrow(() -> lock.acquire().toCompletableFuture().join());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldFailAcquireLockIfOtherProposalExists(final boolean expiring) throws Exception {
        final Optional<Instant> expiration;
        if (expiring) {
            expiration = Optional.of(Instant.now().plus(Duration.ofHours(1)));
        } else {
            expiration = Optional.empty();
        }
        final String uuid = UUID.randomUUID().toString();
        final Key proposal = new Key.From(new Proposals.RootKey(this.target), uuid);
        new BlockingStorage(this.storage).save(
            proposal,
            expiration.map(Instant::toString).orElse("").getBytes()
        );
        final StorageLock lock = new StorageLock(this.storage, this.target);
        final CompletionException exception = Assertions.assertThrows(
            CompletionException.class,
            () -> lock.acquire().toCompletableFuture().join(),
            "Fails to acquire"
        );
        MatcherAssert.assertThat(
            "Reason for failure is IllegalStateException",
            exception.getCause(),
            new IsInstanceOf(ArtipieIOException.class)
        );
        MatcherAssert.assertThat(
            "Proposals unmodified",
            this.storage.list(new Proposals.RootKey(this.target))
                .toCompletableFuture().join()
                .stream()
                .map(Key::string)
                .collect(Collectors.toList()),
            Matchers.contains(proposal.string())
        );
    }

    @Test
    void shouldAcquireLockIfOtherExpiredProposalExists() throws Exception {
        final String uuid = UUID.randomUUID().toString();
        new BlockingStorage(this.storage).save(
            new Key.From(new Proposals.RootKey(this.target), uuid),
            Instant.now().plus(Duration.ofHours(1)).toString().getBytes()
        );
        final StorageLock lock = new StorageLock(this.storage, this.target, uuid, Optional.empty());
        Assertions.assertDoesNotThrow(() -> lock.acquire().toCompletableFuture().join());
    }

    @Test
    void shouldRemoveProposalOnRelease() {
        final String uuid = UUID.randomUUID().toString();
        final Key proposal = new Key.From(new Proposals.RootKey(this.target), uuid);
        this.storage.save(proposal, Content.EMPTY).toCompletableFuture().join();
        new StorageLock(this.storage, this.target, uuid, Optional.empty())
            .release()
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.storage.exists(proposal).toCompletableFuture().join(),
            new IsEqual<>(false)
        );
    }

    /**
     * Storage with one extra "phantom" key.
     * This key present in `list` method results, but cannot be found otherwise.
     * Class is designed to test cases when key returned by list and then deleted concurrently,
     * so it is not found when accessed by `value` method later.
     *
     * @since 0.28
     */
    private static class PhantomKeyStorage implements Storage {

        /**
         * Origin storage.
         */
        private final Storage storage;

        /**
         * Phantom key.
         */
        private final Key phantom;

        /**
         * Ctor.
         *
         * @param storage Origin storage.
         * @param phantom Phantom key.
         */
        PhantomKeyStorage(final Storage storage, final Key phantom) {
            this.storage = storage;
            this.phantom = phantom;
        }

        @Override
        public CompletableFuture<Boolean> exists(final Key key) {
            return this.storage.exists(key);
        }

        @Override
        public CompletableFuture<Collection<Key>> list(final Key prefix) {
            return this.storage.list(prefix).thenApply(
                keys -> {
                    final Collection<Key> copy = new ArrayList<>(keys);
                    copy.add(this.phantom);
                    return copy;
                }
            );
        }

        @Override
        public CompletableFuture<Void> save(final Key key, final Content content) {
            return this.storage.save(key, content);
        }

        @Override
        public CompletableFuture<Void> move(final Key source, final Key destination) {
            return this.storage.move(source, destination);
        }

        @Override
        @SuppressWarnings("deprecation")
        public CompletableFuture<Long> size(final Key key) {
            return this.storage.size(key);
        }

        @Override
        public CompletableFuture<? extends Meta> metadata(final Key key) {
            return this.storage.metadata(key);
        }

        @Override
        public CompletableFuture<Content> value(final Key key) {
            return this.storage.value(key);
        }

        @Override
        public CompletableFuture<Void> delete(final Key key) {
            return this.storage.delete(key);
        }

        @Override
        public <T> CompletionStage<T> exclusively(
            final Key key,
            final Function<Storage, CompletionStage<T>> operation
        ) {
            return this.storage.exclusively(key, operation);
        }
    }
}

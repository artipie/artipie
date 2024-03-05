/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.lock.storage;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ValueNotFoundException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Proposals for acquiring storage lock.
 *
 * @since 0.24
 */
final class Proposals {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Target key.
     */
    private final Key target;

    /**
     * Ctor.
     *
     * @param storage Storage.
     * @param target Target key.
     */
    Proposals(final Storage storage, final Key target) {
        this.storage = storage;
        this.target = target;
    }

    /**
     * Create proposal with specified UUID.
     *
     * @param uuid UUID.
     * @param expiration Expiration time.
     * @return Completion of proposal create operation.
     */
    public CompletionStage<Void> create(final String uuid, final Optional<Instant> expiration) {
        return this.storage.save(
            this.proposalKey(uuid),
            expiration.<Content>map(
                instant -> new Content.From(instant.toString().getBytes(StandardCharsets.US_ASCII))
            ).orElse(Content.EMPTY)
        );
    }

    /**
     * Check that there is single proposal with specified UUID.
     *
     * @param uuid UUID.
     * @return Completion of proposals check operation.
     */
    public CompletionStage<Void> checkSingle(final String uuid) {
        final Instant now = Instant.now();
        final Key own = this.proposalKey(uuid);
        return this.storage.list(new RootKey(this.target)).thenCompose(
            proposals -> CompletableFuture.allOf(
                proposals.stream()
                    .filter(key -> !key.equals(own))
                    .map(
                        proposal -> this.valueIfPresent(proposal).thenCompose(
                            value -> value.map(
                                content -> content.asStringFuture().thenCompose(
                                    expiration -> {
                                        if (isNotExpired(expiration, now)) {
                                            throw new ArtipieIOException(
                                                String.join(
                                                    "\n",
                                                    "Failed to acquire lock.",
                                                    String.format("Own: `%s`", own),
                                                    String.format(
                                                        "Others: %s",
                                                        proposals.stream()
                                                            .map(Key::toString)
                                                            .map(str -> String.format("`%s`", str))
                                                            .collect(Collectors.joining(", "))
                                                    ),
                                                    String.format(
                                                        "Not expired: `%s` `%s`",
                                                        proposal,
                                                        expiration
                                                    )
                                                )
                                            );
                                        }
                                        return CompletableFuture.allOf();
                                    }
                                )
                            ).orElse(CompletableFuture.allOf())
                        )
                    )
                    .toArray(CompletableFuture[]::new)
            )
        );
    }

    /**
     * Delete proposal with specified UUID.
     *
     * @param uuid UUID.
     * @return Completion of proposal delete operation.
     */
    public CompletionStage<Void> delete(final String uuid) {
        return this.storage.delete(this.proposalKey(uuid));
    }

    /**
     * Construct proposal key with specified UUID.
     *
     * @param uuid UUID.
     * @return Proposal key.
     */
    private Key proposalKey(final String uuid) {
        return new Key.From(new RootKey(this.target), uuid);
    }

    /**
     * Checks that instant in string format is not expired, e.g. is after current time.
     * Empty string considered to never expire.
     *
     * @param instant Instant in string format.
     * @param now Current time.
     * @return True if instant is not expired, false - otherwise.
     */
    private static boolean isNotExpired(final String instant, final Instant now) {
        return instant.isEmpty() || Instant.parse(instant).isAfter(now);
    }

    /**
     * Loads value content is it is present.
     *
     * @param key Key for the value.
     * @return Content if value presents, empty otherwise.
     */
    private CompletableFuture<Optional<Content>> valueIfPresent(final Key key) {
        final CompletableFuture<Optional<Content>> value = this.storage.value(key)
            .thenApply(Optional::of);
        return value.handle(
            (content, throwable) -> {
                final CompletableFuture<Optional<Content>> result;
                if (throwable != null && throwable.getCause() instanceof ValueNotFoundException) {
                    result = CompletableFuture.completedFuture(Optional.empty());
                } else {
                    result = value;
                }
                return result;
            }
        ).thenCompose(Function.identity());
    }

    /**
     * Root key for lock proposals.
     *
     * @since 0.24
     */
    static class RootKey extends Key.Wrap {

        /**
         * Ctor.
         *
         * @param target Target key.
         */
        protected RootKey(final Key target) {
            super(new From(new From(".artipie-locks"), new From(target)));
        }
    }
}

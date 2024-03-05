/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.asto.etcd;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.asto.UnderLockOperation;
import com.artipie.asto.ValueNotFoundException;
import com.artipie.asto.ext.CompletableFutureSupport;
import com.artipie.asto.lock.storage.StorageLock;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.GetOption.SortOrder;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Etcd based storage.
 * Main purpose of this storage is to be used as Artipie configuration main storage
 * for distributed cluster setup.
 * <p>
 * This storage loads content data into memory first, therefore
 * it has content size limitation for 10Mb. So it requires the client to
 * provide sized content.
 * </p>
 * @since 0.1
 */
public final class EtcdStorage implements Storage {

    /**
     * Reject content greater that 10MB.
     */
    private static final long MAX_SIZE = 1024 * 1024 * 10;

    /**
     * Etcd root key.
     */
    private static final ByteSequence ETCD_ROOT_KEY =
        ByteSequence.from("\0", StandardCharsets.UTF_8);

    /**
     * Etcd client.
     */
    private final Client client;

    /**
     * Storage identifier: endpoints of this storage etcd client.
     */
    private final String id;

    /**
     * Ctor.
     *
     * @param client Etcd client
     * @param endpoints Endpoints of this storage etcd client
     */
    public EtcdStorage(final Client client, final String endpoints) {
        this.client = client;
        this.id = String.format("Etcd: %s", endpoints);
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        return this.client.getKVClient().get(
            keyToSeq(key),
            GetOption.newBuilder().withCountOnly(true).build()
        ).thenApply(rsp -> rsp.getCount() > 0);
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key prefix) {
        final CompletableFuture<GetResponse> future;
        if (prefix.equals(Key.ROOT)) {
            future = this.client.getKVClient().get(
                EtcdStorage.ETCD_ROOT_KEY,
                GetOption.newBuilder()
                    .withKeysOnly(true)
                    .withSortOrder(SortOrder.ASCEND)
                    .withRange(EtcdStorage.ETCD_ROOT_KEY)
                    .build()
            );
        } else {
            future = this.client.getKVClient().get(
                keyToSeq(prefix),
                GetOption.newBuilder()
                    .withKeysOnly(true)
                    .withSortOrder(SortOrder.ASCEND)
                    .isPrefix(true)
                    .build()
            );
        }
        return future.thenApply(
            rsp -> rsp.getKvs().stream()
                .map(kv -> new String(kv.getKey().getBytes(), StandardCharsets.UTF_8))
                .map(Key.From::new)
                .distinct()
                .collect(Collectors.toList())
        );
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public CompletableFuture<Void> save(final Key key, final Content content) {
        final long size = content.size().orElse(0L);
        if (size < 0 || size > EtcdStorage.MAX_SIZE) {
            return new CompletableFutureSupport.Failed<Void>(
                new ArtipieIOException(
                    String.format("Content size must be in range (0;%d)", EtcdStorage.MAX_SIZE)
                )
            ).get();
        }
        return content.asBytesFuture()
            .thenApply(ByteSequence::from)
            .thenCompose(data -> this.client.getKVClient().put(keyToSeq(key), data))
            .thenApply(ignore -> (Void) null).toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        return this.value(source)
            .thenCompose(data -> this.save(destination, data))
            .thenCompose(none -> this.delete(source));
    }

    @Override
    public CompletableFuture<? extends Meta> metadata(final Key key) {
        return this.client.getKVClient().get(keyToSeq(key)).thenApply(
            rsp -> rsp.getKvs().stream().max(
                Comparator.comparingLong(KeyValue::getVersion)
            )
        ).thenApply(
            kv -> new EtcdMeta(kv.orElseThrow(() -> new ValueNotFoundException(key)))
        );
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        return this.client.getKVClient().get(keyToSeq(key)).thenApply(
            rsp -> rsp.getKvs().stream().max(
                Comparator.comparingLong(KeyValue::getVersion)
            )
        ).thenApply(
            kv -> {
                byte[] data = kv.orElseThrow(() -> new ValueNotFoundException(key)).getValue().getBytes();
                return new Content.OneTime(new Content.From(data));
            }
        );
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        return this.client.getKVClient().delete(keyToSeq(key)).thenAccept(
            rsp -> {
                if (rsp.getDeleted() == 0) {
                    throw new ValueNotFoundException(key);
                }
            }
        );
    }

    @Override
    public <T> CompletionStage<T> exclusively(final Key key,
        final Function<Storage, CompletionStage<T>> operation) {
        return new UnderLockOperation<>(new StorageLock(this, key), operation).perform(this);
    }

    @Override
    public String identifier() {
        return this.id;
    }

    /**
     * Convert asto key to ectd bytes.
     * @param key Asto key
     * @return Etcd byte sequence
     */
    private static ByteSequence keyToSeq(final Key key) {
        return ByteSequence.from(key.string(), StandardCharsets.UTF_8);
    }
}

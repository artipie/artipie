/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.npm.misc.JsonFromPublisher;
import hu.akarnokd.rxjava2.interop.CompletableInterop;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.json.JsonObject;

/**
 * The NPM front.
 * The main goal is to consume a json uploaded by
 * {@code npm publish command} and to:
 *  1. to generate source archives
 *  2. meta.json file
 *
 * @since 0.1
 * @deprecated Use {@link Publish} implementations from `http` package.
 */
@Deprecated
public class Npm {

    /**
     * The storage.
     */
    private final RxStorage storage;

    /**
     * Constructor.
     * @param storage The storage.
     */
    public Npm(final Storage storage) {
        this.storage = new RxStorageWrapper(storage);
    }

    /**
     * Constructor.
     * @param storage The storage.
     * @param pathref The sources archive pathpref. Example: http://host:8080. Unused since 0.6
     * @deprecated Use {@link #Npm(Storage)} instead
     */
    @Deprecated
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public Npm(final Storage storage, final Optional<String> pathref) {
        this(storage);
    }

    /**
     * Publish file.
     * @param prefix Prefix
     * @param artifact Artifact
     * @return Completable action
     */
    public final CompletableFuture<Void> publish(final Key prefix, final Key artifact) {
        return this.storage.value(artifact)
            .map(JsonFromPublisher::new)
            .flatMap(JsonFromPublisher::jsonRx)
            .flatMapCompletable(
                uploaded -> this.updateMetaFile(prefix, uploaded)
                    .andThen(this.updateSourceArchives(uploaded))
            ).to(CompletableInterop.await())
            .<Void>thenApply(r -> null)
            .toCompletableFuture();
    }

    /**
     * Updates the meta.json file based on tgz package file.
     * @param prefix Package prefix.
     * @param file Tgz archive file.
     * @return Completion or error signal.
     */
    public Completable updateMetaFile(final Key prefix, final TgzArchive file) {
        throw new UnsupportedOperationException();
    }

    /**
     * Generate .tgz archives extracted from the uploaded json.
     *
     * @param uploaded The uploaded json
     * @return Completion or error signal.
     */
    private Completable updateSourceArchives(
        final JsonObject uploaded
    ) {
        return Single.fromCallable(() -> uploaded.getJsonObject("_attachments"))
            .flatMapCompletable(
                attachments ->
                    Completable.concat(attachments.keySet().stream()
                        .map(
                            attachment -> {
                                final byte[] bytes = new TgzArchive(
                                    attachments.getJsonObject(attachment).getString("data")
                                ).bytes();
                                return this.storage.save(
                                    new Key.From(
                                        uploaded.getString("name"),
                                        "-",
                                        attachment
                                    ),
                                    new Content.From(bytes)
                                );
                            }
                        ).collect(Collectors.toList())
                    )
            );
    }

    /**
     * Update the meta.json file.
     *
     * @param prefix The package prefix
     * @param uploaded The uploaded json
     * @return Completion or error signal.
     */
    private Completable updateMetaFile(
        final Key prefix,
        final JsonObject uploaded) {
        final Key metafilename = new Key.From(prefix, "meta.json");
        return this.storage.exists(metafilename)
            .flatMap(
                exists -> {
                    final Single<Meta> meta;
                    if (exists) {
                        meta = this.storage.value(metafilename)
                            .map(JsonFromPublisher::new)
                            .flatMap(JsonFromPublisher::jsonRx)
                            .map(Meta::new);
                    } else {
                        meta = Single.just(
                            new Meta(
                                new NpmPublishJsonToMetaSkelethon(uploaded).skeleton()
                            )
                        );
                    }
                    return meta;
                })
            .map(meta -> meta.updatedMeta(uploaded))
            .flatMapCompletable(
                meta -> this.storage.save(
                    metafilename, new Content.From(
                        meta.toString().getBytes(StandardCharsets.UTF_8)
                    )
                )
            );
    }
}

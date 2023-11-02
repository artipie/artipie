/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.npm.MetaUpdate;
import com.artipie.npm.Publish;
import com.artipie.npm.TgzArchive;
import com.artipie.npm.misc.JsonFromPublisher;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import javax.json.JsonObject;

/**
 * The NPM publish front.
 * The main goal is to consume a json uploaded by
 * {@code npm publish command} and to:
 *  1. to generate source archives
 *  2. meta.json file
 *
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class CliPublish implements Publish {
    /**
     * Pattern for `referer` header value.
     */
    public static final Pattern HEADER = Pattern.compile("publish.*");

    /**
     * Attachments json field name.
     */
    private static final String ATTACHMENTS = "_attachments";

    /**
     * The storage.
     */
    private final Storage storage;

    /**
     * Constructor.
     * @param storage The storage.
     */
    CliPublish(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletableFuture<Publish.PackageInfo> publishWithInfo(
        final Key prefix, final Key artifact
    ) {
        return this.artifactJson(artifact).thenCompose(
            uploaded -> new MetaUpdate.ByJson(uploaded).update(prefix, this.storage)
                .thenCompose(ignored -> this.updateSourceArchives(uploaded))
                .thenApply(
                    size -> new PackageInfo(
                        prefix.toString(),
                        CliPublish.packageVersion(uploaded), size
                    )
                )
        );
    }

    @Override
    public CompletableFuture<Void> publish(final Key prefix, final Key artifact) {
        return this.artifactJson(artifact).thenCompose(
            uploaded -> new MetaUpdate.ByJson(uploaded).update(prefix, this.storage)
                .thenCompose(ignored -> this.updateSourceArchives(uploaded))
                .thenAccept(size -> { })
        );
    }

    /**
     * Get package json.
     * @param artifact Artifact key
     * @return Completable action with json
     */
    private CompletableFuture<JsonObject> artifactJson(final Key artifact) {
        return this.storage.value(artifact)
            .thenCompose(bytes -> new JsonFromPublisher(bytes).json());
    }

    /**
     * Generate .tgz archives extracted from the uploaded json.
     *
     * @param uploaded The uploaded json
     * @return Completion or error signal.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private CompletableFuture<Long> updateSourceArchives(final JsonObject uploaded) {
        final AtomicLong size = new AtomicLong();
        final Set<String> attachments = uploaded.getJsonObject(CliPublish.ATTACHMENTS).keySet();
        final CompletableFuture<Void>[] array = new CompletableFuture[attachments.size()];
        int ind = 0;
        for (final String file : attachments) {
            final byte[] bytes = new TgzArchive(
                uploaded.getJsonObject(CliPublish.ATTACHMENTS).getJsonObject(file).getString("data")
            ).bytes();
            array[ind] = this.storage.save(
                new Key.From(uploaded.getString("name"), "-", file), new Content.From(bytes)
            );
            ind = ind + 1;
            size.getAndAdd(bytes.length);
        }
        return CompletableFuture.allOf(array).thenApply(ignored -> size.get());
    }

    /**
     * Read version from uploaded json.
     * @param json Uploaded json
     * @return Version
     */
    private static String packageVersion(final JsonObject json) {
        return json.getJsonObject("versions").keySet().stream().findFirst()
            .orElse("ABSENT_VERSION");
    }

}

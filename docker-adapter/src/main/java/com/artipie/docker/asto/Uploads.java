/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Storage;
import com.artipie.docker.RepoName;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Asto implementation of {@link Uploads}.
 */
public final class Uploads {

    private final Storage storage;

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * @param storage Asto storage
     * @param name Repository name
     */
    public Uploads(final Storage storage, final RepoName name) {
        this.storage = storage;
        this.name = name;
    }

    public CompletableFuture<Upload> start() {
        final String uuid = UUID.randomUUID().toString();
        final Upload upload = new Upload(this.storage, this.name, uuid);
        return upload.start().thenApply(ignored -> upload);
    }

    public CompletableFuture<Optional<Upload>> get(final String uuid) {
        final CompletableFuture<Optional<Upload>> result;
        if (uuid.isEmpty()) {
            result = CompletableFuture.completedFuture(Optional.empty());
        } else {
            result = this.storage.list(Layout.upload(this.name, uuid)).thenApply(
                list -> {
                    final Optional<Upload> upload;
                    if (list.isEmpty()) {
                        upload = Optional.empty();
                    } else {
                        upload = Optional.of(
                            new Upload(this.storage, this.name, uuid)
                        );
                    }
                    return upload;
                }
            );
        }
        return result;
    }
}

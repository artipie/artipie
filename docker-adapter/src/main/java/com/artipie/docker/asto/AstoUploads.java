/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Storage;
import com.artipie.docker.RepoName;
import com.artipie.docker.Upload;
import com.artipie.docker.Uploads;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Asto implementation of {@link Uploads}.
 *
 * @since 0.3
 */
public final class AstoUploads implements Uploads {

    /**
     * Asto storage.
     */
    private final Storage asto;

    /**
     * Uploads layout.
     */
    private final UploadsLayout layout;

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Ctor.
     *
     * @param asto Asto storage
     * @param layout Uploads layout.
     * @param name Repository name
     */
    public AstoUploads(final Storage asto, final UploadsLayout layout, final RepoName name) {
        this.asto = asto;
        this.layout = layout;
        this.name = name;
    }

    @Override
    public CompletionStage<Upload> start() {
        final String uuid = UUID.randomUUID().toString();
        final AstoUpload upload = new AstoUpload(this.asto, this.layout, this.name, uuid);
        return upload.start().thenApply(ignored -> upload);
    }

    @Override
    public CompletionStage<Optional<Upload>> get(final String uuid) {
        final CompletableFuture<Optional<Upload>> result;
        if (uuid.isEmpty()) {
            result = CompletableFuture.completedFuture(Optional.empty());
        } else {
            result = this.asto.list(this.layout.upload(this.name, uuid)).thenApply(
                list -> {
                    final Optional<Upload> upload;
                    if (list.isEmpty()) {
                        upload = Optional.empty();
                    } else {
                        upload = Optional.of(
                            new AstoUpload(this.asto, this.layout, this.name, uuid)
                        );
                    }
                    return upload;
                }
            );
        }
        return result;
    }
}

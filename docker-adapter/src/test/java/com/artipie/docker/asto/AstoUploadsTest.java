/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.asto;

import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.RepoName;
import com.artipie.docker.Uploads;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AstoUploads}.
 *
 * @since 0.5
 */
@SuppressWarnings("PMD.TooManyMethods")
final class AstoUploadsTest {
    /**
     * Slice being tested.
     */
    private Uploads uploads;

    /**
     * Storage.
     */
    private Storage storage;

    /**
     * RepoName.
     */
    private RepoName reponame;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
        this.reponame = new RepoName.Valid("test");
        this.uploads = new AstoUploads(
            this.storage,
            new DefaultLayout(),
            this.reponame
        );
    }

    @Test
    void checkUniquenessUuids() {
        final String uuid = this.uploads.start()
            .toCompletableFuture().join()
            .uuid();
        final String otheruuid = this.uploads.start()
            .toCompletableFuture().join()
            .uuid();
        MatcherAssert.assertThat(
            uuid.equals(otheruuid),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldStartNewAstoUpload() {
        final String uuid = this.uploads.start()
            .toCompletableFuture().join()
            .uuid();
        MatcherAssert.assertThat(
            this.storage.list(
                new UploadKey(this.reponame, uuid)
            ).join().isEmpty(),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldFindUploadByUuid() {
        final String uuid = this.uploads.start()
            .toCompletableFuture().join()
            .uuid();
        MatcherAssert.assertThat(
            this.uploads.get(uuid)
                .toCompletableFuture().join()
                .get().uuid(),
            new IsEqual<>(uuid)
        );
    }

    @Test
    void shouldNotFindUploadByEmptyUuid() {
        MatcherAssert.assertThat(
            this.uploads.get("")
                .toCompletableFuture().join()
                .isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldReturnEmptyOptional() {
        MatcherAssert.assertThat(
            this.uploads.get("uuid")
                .toCompletableFuture().join()
                .isPresent(),
            new IsEqual<>(false)
        );
    }
}

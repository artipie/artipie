/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer;

import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AstoRepository#packages()} and {@link AstoRepository#packages(Name)}.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
 */
class AstoRepositoryPackagesTest {

    /**
     * Storage used in tests.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void shouldLoadEmptyPackages() {
        final Name name = new Name("foo/bar");
        MatcherAssert.assertThat(
            new AstoRepository(this.storage).packages(name)
                .toCompletableFuture().join()
                .isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldLoadNonEmptyPackages() throws Exception {
        final Name name = new Name("foo/bar2");
        final byte[] bytes = "some data".getBytes();
        new BlockingStorage(this.storage).save(name.key(), bytes);
        new AstoRepository(this.storage).packages(name).toCompletableFuture().join().get()
            .save(this.storage, name.key())
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            new BlockingStorage(this.storage).value(name.key()),
            new IsEqual<>(bytes)
        );
    }

    @Test
    void shouldLoadEmptyAllPackages() {
        MatcherAssert.assertThat(
            new AstoRepository(this.storage).packages().toCompletableFuture().join().isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void shouldLoadNonEmptyAllPackages() throws Exception {
        final byte[] bytes = "all packages".getBytes();
        new BlockingStorage(this.storage).save(new AllPackages(), bytes);
        new AstoRepository(this.storage).packages().toCompletableFuture().join().get()
            .save(this.storage, new AllPackages())
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            new BlockingStorage(this.storage).value(new AllPackages()),
            new IsEqual<>(bytes)
        );
    }
}

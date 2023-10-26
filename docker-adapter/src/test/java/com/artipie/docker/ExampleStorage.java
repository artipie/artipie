/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker;

import com.artipie.asto.Copy;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;

/**
 * Storage with example docker repository data from resources folder 'example-my-alpine'.
 *
 * @since 0.2
 */
public final class ExampleStorage extends Storage.Wrap {

    /**
     * Ctor.
     */
    public ExampleStorage() {
        super(copy());
    }

    /**
     * Copy example data to new in-memory storage.
     *
     * @return Copied storage.
     */
    private static Storage copy() {
        final Storage target = new InMemoryStorage();
        new Copy(new FileStorage(new TestResource("example-my-alpine").asPath()))
            .copy(target)
            .toCompletableFuture().join();
        return target;
    }
}

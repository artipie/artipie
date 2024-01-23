/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.test;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Splitting;
import com.artipie.asto.Storage;
import io.reactivex.Flowable;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Storage for tests.
 * <p/>
 * Reading a value by a key return content that emit chunks of bytes
 * with random size and random delays.
 *
 * @since 1.12
 */
public class ReadWithDelaysStorage extends Storage.Wrap {
    /**
     * Ctor.
     *
     * @param delegate Original storage.
     */
    public ReadWithDelaysStorage(final Storage delegate) {
        super(delegate);
    }

    @Override
    public final CompletableFuture<Content> value(final Key key) {
        final Random random = new Random();
        return super.value(key)
            .thenApply(
                content -> new Content.From(
                    Flowable.fromPublisher(content)
                        .flatMap(
                            buffer -> new Splitting(
                                buffer,
                                (random.nextInt(9) + 1) * 1024
                            ).publisher()
                        )
                        .delay(random.nextInt(5_000), TimeUnit.MILLISECONDS)
                )
            );
    }
}

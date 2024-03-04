/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.ext;

import com.artipie.asto.Content;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Rx publisher transformer to single.
 * @param <T> Single type
 * @since 0.33
 */
public final class ContentAs<T>
    implements Function<Single<? extends Publisher<ByteBuffer>>, Single<? extends T>> {

    /**
     * Content as string.
     */
    public static final ContentAs<String> STRING = new ContentAs<>(
        bytes -> new String(bytes, StandardCharsets.UTF_8)
    );

    /**
     * Content as {@code long} number.
     */
    public static final ContentAs<Long> LONG = new ContentAs<>(
        bytes -> Long.valueOf(new String(bytes, StandardCharsets.US_ASCII))
    );

    /**
     * Content as {@code bytes}.
     */
    public static final ContentAs<byte[]> BYTES = new ContentAs<>(bytes -> bytes);

    /**
     * Transform function.
     */
    private final Function<byte[], T> transform;

    /**
     * Ctor.
     * @param transform Transform function
     */
    public ContentAs(final Function<byte[], T> transform) {
        this.transform = transform;
    }

    @Override
    public Single<? extends T> apply(
        final Single<? extends Publisher<ByteBuffer>> content
    ) {
        return content.flatMap(
            pub -> Single.fromFuture(new Content.From(pub).asBytesFuture())
        ).map(this.transform);
    }
}

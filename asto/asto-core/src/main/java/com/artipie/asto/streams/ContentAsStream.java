/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.streams;

import com.artipie.asto.ArtipieIOException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import org.cqfn.rio.WriteGreed;
import org.cqfn.rio.stream.ReactiveOutputStream;
import org.reactivestreams.Publisher;

/**
 * Process content as input stream.
 * This class allows to treat storage item as input stream and
 * perform some action with this stream (read/uncompress/parse etc).
 * @param <T> Result type
 * @since 1.4
 */
public final class ContentAsStream<T> {

    /**
     * Publisher to process.
     */
    private final Publisher<ByteBuffer> content;

    /**
     * Ctor.
     * @param content Content
     */
    public ContentAsStream(final Publisher<ByteBuffer> content) {
        this.content = content;
    }

    /**
     * Process storage item as input stream by performing provided action on it.
     * @param action Action to perform
     * @return Completion action with the result
     */
    public CompletionStage<T> process(final Function<InputStream, T> action) {
        return CompletableFuture.supplyAsync(
            () -> {
                try (
                    PipedInputStream in = new PipedInputStream();
                    PipedOutputStream out = new PipedOutputStream(in)
                ) {
                    final CompletionStage<Void> ros =
                        new ReactiveOutputStream(out).write(this.content, WriteGreed.SYSTEM);
                    final T result = action.apply(in);
                    return ros.thenApply(nothing -> result);
                } catch (final IOException err) {
                    throw new ArtipieIOException(err);
                }
            }
        ).thenCompose(Function.identity());
    }
}

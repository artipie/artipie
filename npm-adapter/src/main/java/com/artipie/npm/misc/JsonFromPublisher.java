/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.npm.misc;

import com.artipie.asto.Remaining;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import javax.json.Json;
import javax.json.JsonObject;
import org.reactivestreams.Publisher;

/**
 * JsonFromPublisher.
 *
 * @since 0.1
 */
public final class JsonFromPublisher {

    /**
     * Publisher of ByteBuffer.
     */
    private final Publisher<ByteBuffer> bytes;

    /**
     * Ctor.
     *
     * @param bytes Publisher of byte buffer
     */
    public JsonFromPublisher(final Publisher<ByteBuffer> bytes) {
        this.bytes = bytes;
    }

    /**
     * Gets json from publisher.
     *
     * @return Rx Json.
     */
    public Single<JsonObject> jsonRx() {
        final ByteArrayOutputStream content = new ByteArrayOutputStream();
        return Flowable
            .fromPublisher(this.bytes)
            .reduce(
                content,
                (stream, buffer) -> {
                    stream.write(
                        new Remaining(buffer).bytes()
                    );
                    return stream;
                })
            .flatMap(
                stream -> Single.just(
                    Json.createReader(
                        new ByteArrayInputStream(
                            stream.toByteArray()
                        )
                    ).readObject()
                )
            );
    }

    /**
     * Gets json from publisher.
     *
     * @return Completable future Json.
     */
    public CompletableFuture<JsonObject> json() {
        return this.jsonRx()
            .to(SingleInterop.get())
            .toCompletableFuture();
    }
}

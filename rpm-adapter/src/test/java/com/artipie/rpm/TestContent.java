/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm;

import com.artipie.asto.Content;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Single;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Subscriber;

/**
 * Content implementation for test resources.
 * @since 0.6
 */
public final class TestContent implements Content {

    /**
     * Default buffer 8K.
     */
    private static final int BUF_SIZE = 1024 * 8;

    /**
     * Resource name.
     */
    private final String name;

    /**
     * Content for test resource.
     * @param name Resource name
     */
    public TestContent(final String name) {
        this.name = name;
    }

    @Override
    public Optional<Long> size() {
        return Optional.empty();
    }

    @Override
    public void subscribe(final Subscriber<? super ByteBuffer> subscriber) {
        Single.just(Thread.currentThread().getContextClassLoader())
            .flatMapPublisher(
                ctx -> Flowable.create(
                    (FlowableOnSubscribe<ByteBuffer>) emitter -> {
                        final byte[] buf = new byte[TestContent.BUF_SIZE];
                        try (InputStream is = ctx.getResourceAsStream(this.name)) {
                            if (is == null) {
                                emitter.onError(
                                    new IllegalArgumentException(
                                        String.format("resource %s doesn't exist", this.name)
                                    )
                                );
                                return;
                            }
                            while (is.read(buf) > 0) {
                                emitter.onNext(ByteBuffer.wrap(buf));
                            }
                        } catch (final IOException err) {
                            emitter.onError(err);
                        }
                        emitter.onComplete();
                    },
                    BackpressureStrategy.BUFFER
                )
            ).subscribe(subscriber);
    }
}

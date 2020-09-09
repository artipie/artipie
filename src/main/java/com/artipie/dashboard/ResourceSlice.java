/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.dashboard;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

/**
 * Classpath resource content.
 * @since 0.6
 */
public final class ResourceSlice implements Slice {

    /**
     * Classloader.
     */
    private final ClassLoader clo;

    /**
     * Resource name.
     */
    private final String name;

    /**
     * New resource from current thread context classloader.
     * @param name Resource name
     */
    public ResourceSlice(final String name) {
        this(Thread.currentThread().getContextClassLoader(), name);
    }

    /**
     * New resource from provided classloader.
     * @param clo Classloader
     * @param name Resource name
     */
    public ResourceSlice(final ClassLoader clo, final String name) {
        this.clo = clo;
        this.name = name;
    }

    @Override
    public Response response(final String lien, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final InputStream stream = this.clo.getResourceAsStream(this.name);
        final Response rsp;
        if (stream == null) {
            rsp = new RsWithStatus(
                new RsWithBody(
                    String.format("Resource '%s' not found", this.name), StandardCharsets.UTF_8
                ),
                RsStatus.NOT_FOUND
            );
        } else {
            rsp = new RsWithStatus(
                new RsWithBody(new StreamPublisher(stream)),
                RsStatus.OK
            );
        }
        return rsp;
    }

    /**
     * Input stream as a publisher.
     * @since 0.6
     */
    private static final class StreamPublisher extends Flowable<ByteBuffer> {

        /**
         * Buffer size for resources.
         */
        private static final int BUF_SIZE = 1024 * 8;

        /**
         * Input stream.
         */
        private final InputStream stream;

        /**
         * New publisher from stream.
         * @param stream Input stream
         */
        StreamPublisher(final InputStream stream) {
            this.stream = stream;
        }

        @Override
        public void subscribeActual(final Subscriber<? super ByteBuffer> sub) {
            Flowable.generate(
                (Consumer<Emitter<ByteBuffer>>) emitter -> {
                    final byte[] buf = new byte[StreamPublisher.BUF_SIZE];
                    final int read;
                    try {
                        read = this.stream.read(buf);
                    } catch (final IOException err) {
                        emitter.onError(err);
                        return;
                    }
                    if (read < 0) {
                        emitter.onComplete();
                    } else {
                        emitter.onNext(ByteBuffer.wrap(buf));
                    }
                }
            ).observeOn(Schedulers.io()).doOnTerminate(this.stream::close).subscribe(sub);
        }
    }
}

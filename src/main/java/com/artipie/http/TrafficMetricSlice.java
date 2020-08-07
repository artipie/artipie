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
package com.artipie.http;

import com.artipie.http.rs.RsStatus;
import com.artipie.metrics.Metrics;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Calculated uploaded and downloaded body size for all requests.
 * @since 0.10
 */
public final class TrafficMetricSlice implements Slice {

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Metrics to update.
     */
    private final Metrics metrics;

    /**
     * Update traffic metrics on requests and responses.
     * @param origin Origin slice to decorate
     * @param metrics Metrics to update
     */
    public TrafficMetricSlice(final Slice origin, final Metrics metrics) {
        this.origin = origin;
        this.metrics = metrics;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> head,
        final Publisher<ByteBuffer> body) {
        return new MetricsResponse(
            this.origin.response(
                line, head,
                new MetricsPublisher(body, this.metrics, "request.body.size")
            ),
            this.metrics
        );
    }

    /**
     * Response which sends itself to connection with metrics.
     * @since 0.10
     */
    private static final class MetricsResponse implements Response {

        /**
         * Origin response.
         */
        private final Response origin;

        /**
         * Metrics to update.
         */
        private final Metrics metrics;

        /**
         * Wraps response.
         * @param response Origin response
         * @param metrics Metrics to update
         */
        MetricsResponse(final Response response, final Metrics metrics) {
            this.origin = response;
            this.metrics = metrics;
        }

        @Override
        public CompletionStage<Void> send(final Connection connection) {
            return this.origin.send(new MetricsConnection(connection, this.metrics));
        }

        /**
         * Response connection which updates metrics on accept.
         * @since 0.10
         */
        private static final class MetricsConnection implements Connection {

            /**
             * Origin connection.
             */
            private final Connection origin;

            /**
             * Metrics to update.
             */
            private final Metrics metrics;

            /**
             * Wrap connection.
             * @param origin Origin connection
             * @param metrics Metrics to send
             */
            MetricsConnection(final Connection origin, final Metrics metrics) {
                this.origin = origin;
                this.metrics = metrics;
            }

            @Override
            public CompletionStage<Void> accept(final RsStatus status, final Headers headers,
                final Publisher<ByteBuffer> body) {
                return this.origin.accept(
                    status, headers,
                    new MetricsPublisher(body, this.metrics, "response.body.size")
                );
            }
        }
    }

    /**
     * Byte-buffer publisher which decorates subscriber on subscribe with
     * {@link MetricsSubscriber}.
     * @since 0.10
     */
    private static final class MetricsPublisher implements Publisher<ByteBuffer> {

        /**
         * Origin publisher.
         */
        private final Publisher<ByteBuffer> origin;

        /**
         * Metrics to update.
         */
        private final Metrics metrics;

        /**
         * Metrics name to update.
         */
        private final String name;

        /**
         * Wrap publisher.
         * @param origin Origin publisher
         * @param metrics Metrics to update
         * @param name Metrics name to update
         */
        MetricsPublisher(final Publisher<ByteBuffer> origin, final Metrics metrics,
            final String name) {
            this.origin = origin;
            this.metrics = metrics;
            this.name = name;
        }

        @Override
        public void subscribe(final Subscriber<? super ByteBuffer> subscriber) {
            this.origin.subscribe(new MetricsSubscriber(subscriber, this.metrics, this.name));
        }

        /**
         * Byte-buffer subscriber which updates metrics on each chunk by using remaining amount of
         * bytes in buffer.
         * @since 0.10
         */
        private static final class MetricsSubscriber implements Subscriber<ByteBuffer> {

            /**
             * Origin subscriber.
             */
            private final Subscriber<? super ByteBuffer> origin;

            /**
             * Metrics to update.
             */
            private final Metrics metrics;

            /**
             * Metrics name to update.
             */
            private final String name;

            /**
             * Wrap subscriber.
             * @param origin Origin subscriber
             * @param metrics Metrics to update
             * @param name Metric name to update
             */
            MetricsSubscriber(final Subscriber<? super ByteBuffer> origin,
                final Metrics metrics, final String name) {
                this.origin = origin;
                this.metrics = metrics;
                this.name = name;
            }

            @Override
            public void onSubscribe(final Subscription subscription) {
                this.origin.onSubscribe(subscription);
            }

            @Override
            public void onNext(final ByteBuffer buffer) {
                this.metrics.counter(this.name).add(buffer.remaining());
                this.origin.onNext(buffer);
            }

            @Override
            public void onError(final Throwable err) {
                this.origin.onError(err);
            }

            @Override
            public void onComplete() {
                this.origin.onComplete();
            }
        }
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http.client.jetty;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.Request;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.reactive.client.ReactiveRequest;
import org.eclipse.jetty.reactive.client.ReactiveResponse;
import org.reactivestreams.Publisher;

/**
 * ClientSlices implementation using Jetty HTTP client as back-end.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class JettyClientSlice implements Slice {

    /**
     * HTTP client.
     */
    private final HttpClient client;

    /**
     * Secure connection flag.
     */
    private final boolean secure;

    /**
     * Host name.
     */
    private final String host;

    /**
     * Port.
     */
    private final int port;

    /**
     * Ctor.
     *
     * @param client HTTP client.
     * @param secure Secure connection flag.
     * @param host Host name.
     * @param port Port.
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    JettyClientSlice(
        final HttpClient client,
        final boolean secure,
        final String host,
        final int port
    ) {
        this.client = client;
        this.secure = secure;
        this.host = host;
        this.port = port;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new AsyncResponse(
            Flowable.fromPublisher(
                this.request(line, headers, body).response(
                    (response, rsbody) -> Flowable.just(
                        (Response) connection -> {
                            final ClosablePublisher closable = new ClosablePublisher(rsbody);
                            final RsFull origin = new RsFull(
                                new RsStatus.ByCode(response.getStatus()).find(),
                                new ResponseHeaders(response),
                                Flowable.fromPublisher(closable).map(
                                    chunk -> {
                                        final ByteBuffer buf = chunk.getByteBuffer();
                                        chunk.release();
                                        return buf;
                                    }
                                ).doOnError(error -> Logger.error(this, "Error on pub"))
                            );
                            return origin.send(connection).handle(
                                (nothing, throwable) -> {
                                    final CompletableFuture<Void> original;
                                    if (throwable == null) {
                                        original = CompletableFuture.allOf();
                                    } else {
                                        original = new CompletableFuture<>();
                                        original.completeExceptionally(throwable);
                                    }
                                    return closable.close().thenCompose(nthng -> original);
                                }
                            ).thenCompose(Function.identity());
                        }
                    )
                )
            ).singleOrError().to(SingleInterop.get())
        );
    }

    /**
     * Create request.
     *
     * @param line Request line.
     * @param headers Request headers.
     * @param body Request body.
     * @return Request built from parameters.
     */
    private ReactiveRequest request(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final RequestLineFrom req = new RequestLineFrom(line);
        final String scheme;
        if (this.secure) {
            scheme = "https";
        } else {
            scheme = "http";
        }
        final URI uri = req.uri();
        final Request request = this.client.newRequest(
            new URIBuilder()
                .setScheme(scheme)
                .setHost(this.host)
                .setPort(this.port)
                .setPath(uri.getPath())
                .setCustomQuery(uri.getQuery())
                .toString()
        ).method(req.method().value());
        Optional<String> type = Optional.empty();
        for (final Map.Entry<String, String> header : headers) {
            request.headers(mutable -> mutable.add(header.getKey(), header.getValue()));
            if (header.getKey().equalsIgnoreCase("content-type")) {
                type = Optional.of(header.getValue());
            }
        }
        final ReactiveRequest res;
        if (req.method() == RqMethod.HEAD) {
            res = ReactiveRequest.newBuilder(request).build();
        } else {
            final Flowable<Content.Chunk> content = Flowable.concat(
                Flowable.fromPublisher(body).map(buffer -> Content.Chunk.from(buffer, false)),
                Flowable.just(Content.Chunk.from(ByteBuffer.wrap(new byte[]{}), true))
            );
            res = ReactiveRequest.newBuilder(request)
                .content(ReactiveRequest.Content.fromPublisher(content, type.orElse("*"))).build();
        }
        return res;
    }

    /**
     * Headers from {@link ReactiveResponse}.
     *
     * @since 0.1
     */
    private static class ResponseHeaders extends Headers.Wrap {

        /**
         * Ctor.
         *
         * @param response Response to extract headers from.
         */
        ResponseHeaders(final ReactiveResponse response) {
            super(
                new Headers.From(
                    response.getHeaders().stream()
                        .map(header -> new Header(header.getName(), header.getValue()))
                        .collect(Collectors.toList())
                )
            );
        }
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.vertx;

import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import java.io.Closeable;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

/**
 * Vert.x Slice.
 *
 * @since 0.1
 */
public final class VertxSliceServer implements Closeable {

    /**
     * The Vert.x.
     */
    private final Vertx vertx;

    /**
     * The slice to be served.
     */
    private final Slice served;

    /**
     * Represents options used by an HttpServer instance.
     */
    private final HttpServerOptions options;

    /**
     * The Http server.
     */
    private HttpServer server;

    /**
     * An object to sync on.
     */
    private final Object sync;

    /**
     * Ctor.
     *
     * @param vertx The vertx.
     * @param served The slice to be served.
     */
    public VertxSliceServer(final Vertx vertx, final Slice served) {
        this(vertx, served, new HttpServerOptions().setPort(0));
    }

    /**
     * Ctor.
     *
     * @param served The slice to be served.
     * @param port The port.
     */
    public VertxSliceServer(final Slice served, final Integer port) {
        this(Vertx.vertx(), served, new HttpServerOptions().setPort(port));
    }

    /**
     * Ctor.
     *
     * @param vertx The vertx.
     * @param served The slice to be served.
     * @param port The port.
     */
    public VertxSliceServer(
        final Vertx vertx,
        final Slice served,
        final Integer port
    ) {
        this(vertx, served, new HttpServerOptions().setPort(port));
    }

    /**
     * Ctor.
     *
     * @param vertx The vertx.
     * @param served The slice to be served.
     * @param options The options to use.
     */
    public VertxSliceServer(
        final Vertx vertx,
        final Slice served,
        final HttpServerOptions options
    ) {
        this.vertx = vertx;
        this.served = served;
        this.options = options;
        this.sync = new Object();
    }

    /**
     * Start the server.
     *
     * @return Port the server is listening on.
     */
    public int start() {
        synchronized (this.sync) {
            if (this.server != null) {
                throw new IllegalStateException("Server was already started");
            }
            this.server = this.vertx.createHttpServer(this.options);
            this.server.requestHandler(this.proxyHandler());
            this.server.rxListen().blockingGet();
            return this.server.actualPort();
        }
    }

    /**
     * Stop the server.
     */
    public void stop() {
        synchronized (this.sync) {
            this.server.rxClose().blockingAwait();
        }
    }

    @Override
    public void close() {
        this.stop();
    }

    /**
     * A handler which proxy incoming requests to encapsulated slice.
     * @return The request handler.
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private Handler<HttpServerRequest> proxyHandler() {
        return (HttpServerRequest req) -> {
            try {
                this.serve(req).exceptionally(
                    throwable -> {
                        VertxSliceServer.sendError(req.response(), throwable);
                        return null;
                    }
                );
            } catch (final Exception ex) {
                VertxSliceServer.sendError(req.response(), ex);
            }
        };
    }

    /**
     * Server HTTP request.
     *
     * @param req HTTP request.
     * @return Completion of request serving.
     */
    private CompletionStage<Void> serve(final HttpServerRequest req) {
        final HttpServerResponse response = req.response();
        return this.served.response(
            new RequestLine(req.method().name(), req.uri(), req.version().toString()).toString(),
            req.headers(),
            req.toFlowable().map(buffer -> ByteBuffer.wrap(buffer.getBytes()))
        ).send(new ContinueConnection(response, new VertxConnection(response)));
    }

    /**
     * Sends response built from {@link Throwable}.
     *
     * @param response Response to write to.
     * @param throwable Exception to send.
     */
    private static void sendError(final HttpServerResponse response, final Throwable throwable) {
        response.setStatusCode(HttpURLConnection.HTTP_INTERNAL_ERROR);
        final StringWriter body = new StringWriter();
        body.append(throwable.toString()).append("\n");
        throwable.printStackTrace(new PrintWriter(body));
        response.end(body.toString());
    }
}

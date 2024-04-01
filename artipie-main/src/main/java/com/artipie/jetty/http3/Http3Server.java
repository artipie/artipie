/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.jetty.http3;

import com.artipie.ArtipieException;
import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.RsStatus;
import io.reactivex.Flowable;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http3.api.Session;
import org.eclipse.jetty.http3.api.Stream;
import org.eclipse.jetty.http3.frames.DataFrame;
import org.eclipse.jetty.http3.frames.HeadersFrame;
import org.eclipse.jetty.http3.server.HTTP3ServerConnector;
import org.eclipse.jetty.http3.server.RawHTTP3ServerConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Http3 server.
 */
public final class Http3Server {

    private static final String HTTP_3 = "HTTP/3";
    private final Slice slice;
    private final Server server;
    private final int port;
    private final SslContextFactory.Server ssl;

    /**
     * @param slice Artipie slice
     * @param port POrt to start server on
     * @param ssl SSL factory
     */
    public Http3Server(final Slice slice, final int port, final SslContextFactory.Server ssl) {
        this.slice = slice;
        this.port = port;
        this.ssl = ssl;
        this.server = new Server();
    }

    /**
     * Starts http3 server.
     * @throws com.artipie.ArtipieException On Error
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void start() {
        final RawHTTP3ServerConnectionFactory factory =
            new RawHTTP3ServerConnectionFactory(new SliceListener());
        factory.getHTTP3Configuration().setStreamIdleTimeout(15_000);
        final HTTP3ServerConnector connector =
            new HTTP3ServerConnector(this.server, this.ssl, factory);
        connector.getQuicConfiguration().setMaxBidirectionalRemoteStreams(1024);
        connector.setPort(this.port);
        try {
            connector.getQuicConfiguration()
                .setPemWorkDirectory(Files.createTempDirectory("http3-pem"));
            this.server.addConnector(connector);
            this.server.start();
        } catch (final Exception err) {
            throw new ArtipieException(err);
        }
    }

    /**
     * Stops the server.
     * @throws Exception On error
     */
    public void stop() throws Exception {
        this.server.stop();
    }

    /**
     * Implementation of {@link Session.Server.Listener} which passes data to slice and sends
     * response to {@link Stream.Server}.
     */
    private final class SliceListener implements Session.Server.Listener {

        @Override
        public Stream.Server.Listener onRequest(Stream.Server stream, HeadersFrame frame) {
            final MetaData.Request request = (MetaData.Request) frame.getMetaData();
            if (frame.isLast()) {
                Http3Server.this.slice.response(
                    new RequestLine(
                        request.getMethod(), request.getHttpURI().getPath(), Http3Server.HTTP_3
                    ),
                    new Headers(
                        request.getHttpFields()
                            .stream()
                            .map(field -> new Header(field.getName(), field.getValue()))
                            .toList()
                    ),
                    Content.EMPTY
                ).thenApply(resp -> send(stream, resp));
                return null;
            } else {
                stream.demand();
                final List<ByteBuffer> buffers = new LinkedList<>();
                return new Stream.Server.Listener() {
                    @Override
                    public void onDataAvailable(final Stream.Server stream) {
                        final Stream.Data data = stream.readData();
                        if (data != null) {
                            final ByteBuffer item = data.getByteBuffer();
                            final ByteBuffer copy = ByteBuffer.allocate(item.capacity());
                            copy.put(item);
                            buffers.add(copy.position(0));
                            data.release();
                            if (data.isLast()) {
                                Http3Server.this.slice.response(
                                    new RequestLine(
                                        request.getMethod(), request.getHttpURI().getPath(),
                                        Http3Server.HTTP_3
                                    ),
                                    new Headers(
                                        request.getHttpFields()
                                            .stream()
                                            .map(field -> new Header(field.getName(), field.getValue()))
                                            .toList()
                                    ),
                                    new Content.From(
                                        Flowable.fromArray(buffers.toArray(ByteBuffer[]::new))
                                    )
                                ).thenApply(resp -> send(stream, resp));
                            }
                        }
                        stream.demand();
                    }
                };
            }
        }
    }

    private static CompletionStage<Void> send(Stream.Server stream, Response artipieRes) {
        RsStatus status = artipieRes.status();
        final MetaData.Response response = new MetaData.Response(
            status.code(), HttpStatus.getMessage(status.code()),
            HttpVersion.HTTP_3,
            HttpFields.from(
                artipieRes.headers().stream()
                    .map(item -> new HttpField(item.getKey(), item.getValue()))
                    .toArray(HttpField[]::new)
            )
        );
        final CompletableFuture<Stream> respond = stream.respond(new HeadersFrame(response, false));
        Flowable.fromPublisher(artipieRes.body())
            .doOnComplete(
                () -> stream.data(new DataFrame(ByteBuffer.wrap(new byte[]{}), true))
            ).forEach(buffer -> stream.data(new DataFrame(buffer, false)));
        //return respond.thenAccept(r -> {        });
        return CompletableFuture.allOf(respond);
    }

}

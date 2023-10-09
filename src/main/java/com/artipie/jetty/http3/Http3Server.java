/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.jetty.http3;

import com.artipie.asto.Content;
import com.artipie.http.Slice;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLine;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.stream.Collectors;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http3.api.Session;
import org.eclipse.jetty.http3.api.Stream;
import org.eclipse.jetty.http3.frames.HeadersFrame;
import org.eclipse.jetty.http3.server.HTTP3ServerConnector;
import org.eclipse.jetty.http3.server.RawHTTP3ServerConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Http3 server.
 * @since 0.31
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
public final class Http3Server {

    /**
     * Artipie slice.
     */
    private final Slice slice;

    /**
     * Http3 server.
     */
    private final Server server;

    /**
     * Port.
     */
    private final int port;

    /**
     * SSL factory.
     */
    private final SslContextFactory.Server ssl;

    /**
     * Ctor.
     *
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
     * @throws Exception On Error
     */
    public void start() throws Exception {
        final RawHTTP3ServerConnectionFactory factory =
            new RawHTTP3ServerConnectionFactory(new SliceListener());
        factory.getHTTP3Configuration().setStreamIdleTimeout(15_000);
        final HTTP3ServerConnector connector =
            new HTTP3ServerConnector(this.server, this.ssl, factory);
        connector.getQuicConfiguration().setMaxBidirectionalRemoteStreams(1024);
        connector.setPort(this.port);
        connector.getQuicConfiguration()
            .setPemWorkDirectory(Files.createTempDirectory("http3-pem"));
        this.server.addConnector(connector);
        this.server.start();
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
     * response to {@link Stream.Server} via {@link  Http3Connection}.
     * @since 0.31
     * @checkstyle ReturnCountCheck (500 lines)
     * @checkstyle AnonInnerLengthCheck (500 lines)
     * @checkstyle NestedIfDepthCheck (500 lines)
     */
    @SuppressWarnings("PMD.OnlyOneReturn")
    private final class SliceListener implements Session.Server.Listener {

        @Override
        public Stream.Server.Listener onRequest(
            final Stream.Server stream, final HeadersFrame frame
        ) {
            final MetaData.Request request = (MetaData.Request) frame.getMetaData();
            if (frame.isLast()) {
                Http3Server.this.slice.response(
                    new RequestLine(
                        request.getMethod(), request.getURIString(), request.getProtocol()
                    ).toString(),
                    request.getFields().stream()
                        .map(field -> new Header(field.getName(), field.getValue()))
                        .collect(Collectors.toList()),
                    Content.EMPTY
                ).send(new Http3Connection(stream)).toCompletableFuture().join();
                return null;
            } else {
                stream.demand();
                final ByteBuffer buffer =
                    ByteBuffer.allocate((int) request.getContentLength());
                return new Stream.Server.Listener() {
                    @Override
                    public void onDataAvailable(final Stream.Server stream) {
                        final Stream.Data data = stream.readData();
                        if (data != null) {
                            buffer.put(data.getByteBuffer());
                            data.complete();
                            if (data.isLast()) {
                                Http3Server.this.slice.response(
                                    new RequestLine(
                                        request.getMethod(), request.getURIString(),
                                        request.getProtocol()
                                    ).toString(),
                                    request.getFields().stream().map(
                                        field -> new Header(field.getName(), field.getValue())
                                    ).collect(Collectors.toList()),
                                    Flowable.fromArray(buffer.position(0))
                                ).send(new Http3Connection(stream));
                            }
                        }
                        stream.demand();
                    }
                };
            }
        }
    }

}

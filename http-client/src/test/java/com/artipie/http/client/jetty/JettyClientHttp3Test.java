/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client.jetty;

import com.artipie.asto.Content;
import com.artipie.asto.Splitting;
import com.artipie.asto.test.TestResource;
import com.artipie.http.Headers;
import com.artipie.http.client.HttpClientSettings;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
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
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test for {@link JettyClientSlices} and http3.
 */
public final class JettyClientHttp3Test {

    /**
     * One data portion.
     */
    private static final String GET_SOME_DATA = "get_one_data_portion";

    /**
     * Two data portions.
     */
    private static final String GET_TWO_DATA_CHUNKS = "get_two_data_chunks";

    /**
     * Size of the two data portions.
     */
    private static final int SIZE = GET_SOME_DATA.getBytes().length
        + GET_TWO_DATA_CHUNKS.getBytes().length;

    /**
     * Client slice.
     */
    private JettyClientSlices client;

    /**
     * Server listener.
     */
    private Session.Server.Listener listener;

    /**
     * Server port.
     */
    private int port;

    @BeforeEach
    void init() throws Exception {
        final Server server = new Server();
        this.listener = new TestListener();
        final SslContextFactory.Server ssl = new SslContextFactory.Server();
        ssl.setKeyStoreType("jks");
        ssl.setKeyStorePath(new TestResource("keystore").asPath().toString());
        ssl.setKeyStorePassword("123456");
        final RawHTTP3ServerConnectionFactory factory =
            new RawHTTP3ServerConnectionFactory(this.listener);
        factory.getHTTP3Configuration().setStreamIdleTimeout(15_000);
        final HTTP3ServerConnector connector =
            new HTTP3ServerConnector(server, ssl, factory);
        connector.getQuicConfiguration().setMaxBidirectionalRemoteStreams(1024);
        connector.getQuicConfiguration()
            .setPemWorkDirectory(Files.createTempDirectory("http3-pem"));
        connector.setPort(0);
        server.addConnector(connector);
        server.start();
        this.client = new JettyClientSlices(
            new HttpClientSettings()
                .setHttp3(true)
                .setTrustAll(true)
        );
        this.client.start();
        this.port = connector.getLocalPort();
    }

    @Test
    void sendGetReceiveData() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        this.client.http("localhost", this.port).response(
            new RequestLine(
                RqMethod.GET.value(), String.format("/%s", GET_SOME_DATA), "HTTP/3"
            ), Headers.EMPTY, Content.EMPTY
        ).thenAccept(response -> {
            latch.countDown();
            Assertions.assertEquals(RsStatus.OK, response.status());
            MatcherAssert.assertThat(
                response.headers(),
                Matchers.contains(
                    new ContentLength(GET_SOME_DATA.getBytes().length)
                )
            );
            Assertions.assertArrayEquals(GET_SOME_DATA.getBytes(), response.body().asBytes());
            latch.countDown();
        });
        Assertions.assertTrue(latch.await(5, TimeUnit.SECONDS), "Response was not received");
    }

    @Test
    void sendGetReceiveTwoDataChunks() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final ByteBuffer expected = ByteBuffer.allocate(SIZE);
        expected.put(GET_SOME_DATA.getBytes());
        expected.put(GET_TWO_DATA_CHUNKS.getBytes());
        final AtomicReference<ByteBuffer> received =
            new AtomicReference<>(ByteBuffer.allocate(SIZE));
        this.client.http("localhost", this.port).response(
            new RequestLine(
                RqMethod.GET.value(), String.format("/%s", GET_TWO_DATA_CHUNKS), "HTTP/3"
            ), Headers.EMPTY, Content.EMPTY
        ).thenAccept(res -> {
            Assertions.assertEquals(RsStatus.OK, res.status());
            MatcherAssert.assertThat(
                res.headers(),
                Matchers.contains(new ContentLength(SIZE))
            );
            Flowable.fromPublisher(res.body())
                .doOnComplete(latch::countDown)
                .forEach(buffer -> received.get().put(buffer));
        });
        Assertions.assertTrue(latch.await(10, TimeUnit.SECONDS), "Response was not received");
        Assertions.assertEquals(expected, received.get());
    }

    @Test
    void chunkedPut() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Random random = new Random();
        final int large = 512 * 512;
        final byte[] data = new byte[large];
        random.nextBytes(data);
        Content body = new Content.From(
            Flowable.fromArray(ByteBuffer.wrap(data)).flatMap(
                buffer -> new Splitting(buffer, (random.nextInt(9) + 1) * 512).publisher()
            ).delay(random.nextInt(1_000), TimeUnit.MILLISECONDS)
        );
        this.client.http("localhost", this.port)
            .response(new RequestLine(RqMethod.PUT.value(), "/any", "HTTP/3"), Headers.EMPTY, body)
            .thenApply(res -> {
                Assertions.assertEquals(RsStatus.OK, res.status());
                latch.countDown();
                return CompletableFuture.completedFuture(null);
            });

        Assertions.assertTrue(latch.await(4, TimeUnit.MINUTES), "Response was not received");
        final ByteBuffer res = ByteBuffer.allocate(large);
        ((TestListener) listener).buffers.forEach(item -> res.put(item.position(0)));
        Assertions.assertArrayEquals(data, res.array());
    }

    /**
     * Test listener.
     */
    private static final class TestListener implements Session.Server.Listener {

        /**
         * Received buffers.
         */
        private final List<ByteBuffer> buffers = new LinkedList<>();

        @Override
        public Stream.Server.Listener onRequest(
            final Stream.Server stream, final HeadersFrame frame
        ) {
            final MetaData.Request request = (MetaData.Request) frame.getMetaData();
            if (frame.isLast()) {
                if (request.getHttpURI().getPath().contains(GET_SOME_DATA)) {
                    stream.respond(
                        new HeadersFrame(getResponse(GET_SOME_DATA.getBytes().length), false)
                    ).thenCompose(
                        item -> item.data(
                            new DataFrame(ByteBuffer.wrap(GET_SOME_DATA.getBytes()), true)
                        )
                    ).join();
                } else if (request.getHttpURI().getPath().contains(GET_TWO_DATA_CHUNKS)) {
                    stream.respond(new HeadersFrame(getResponse(SIZE), false)).thenCompose(
                        item -> item.data(
                            new DataFrame(ByteBuffer.wrap(GET_SOME_DATA.getBytes()), false)
                        )
                    ).thenCompose(
                        item -> item.data(
                            new DataFrame(ByteBuffer.wrap(GET_TWO_DATA_CHUNKS.getBytes()), true)
                        )
                    ).join();
                }
                return null;
            } else {
                stream.demand();
                return new Stream.Server.Listener() {
                    @Override
                    public void onDataAvailable(final Stream.Server stream) {
                        final Stream.Data data = stream.readData();
                        if (data != null) {
                            final ByteBuffer item = data.getByteBuffer();
                            final ByteBuffer copy = ByteBuffer.allocate(item.capacity());
                            copy.put(item);
                            TestListener.this.buffers.add(copy.position(0));
                            data.release();
                            if (data.isLast()) {
                                stream.respond(new HeadersFrame(getResponse(0), true));
                                return;
                            }
                        }
                        stream.demand();
                    }
                };
            }
        }

        private static MetaData.Response getResponse(final int len) {
            return new MetaData.Response(
                HttpStatus.OK_200, HttpStatus.getMessage(HttpStatus.OK_200), HttpVersion.HTTP_3,
                HttpFields.from(new HttpField("content-length", String.valueOf(len)))
            );
        }
    }

}

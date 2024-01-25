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
import com.artipie.http.client.misc.PublisherAs;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link JettyClientSlices} and http3.
 * @since 0.3
 */
@SuppressWarnings(
    {"PMD.AvoidDuplicateLiterals", "PMD.StaticAccessToStaticFields", "PMD.LongVariable"}
)
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
            ).toString(),
            Headers.EMPTY, Content.EMPTY
        ).send(
            (status, headers, publisher) -> {
                latch.countDown();
                MatcherAssert.assertThat(status, new IsEqual<>(RsStatus.OK));
                MatcherAssert.assertThat(
                    headers,
                    Matchers.contains(
                        new Header(
                            "content-length", String.valueOf(GET_SOME_DATA.getBytes().length)
                        )
                    )
                );
                MatcherAssert.assertThat(
                    new PublisherAs(publisher).bytes().toCompletableFuture().join(),
                    new IsEqual<>(GET_SOME_DATA.getBytes())
                );
                latch.countDown();
                return CompletableFuture.allOf();
            }
        );
        MatcherAssert.assertThat("Response was not received", latch.await(5, TimeUnit.SECONDS));
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
            ).toString(),
            Headers.EMPTY, Content.EMPTY
        ).send(
            (status, headers, publisher) -> {
                MatcherAssert.assertThat(status, new IsEqual<>(RsStatus.OK));
                MatcherAssert.assertThat(
                    headers, Matchers.contains(new Header("content-length", String.valueOf(SIZE)))
                );
                Flowable.fromPublisher(publisher).doOnComplete(latch::countDown)
                    .forEach(buffer -> received.get().put(buffer));
                return CompletableFuture.allOf();
            }
        );
        MatcherAssert.assertThat("Response was not received", latch.await(10, TimeUnit.SECONDS));
        MatcherAssert.assertThat(received.get(), new IsEqual<>(expected));
    }

    @Test
    void chunkedPut() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Random random = new Random();
        final int large = 512 * 512;
        final byte[] data = new byte[large];
        random.nextBytes(data);
        this.client.http("localhost", this.port).response(
            new RequestLine(
                RqMethod.PUT.value(), "/any", "HTTP/3"
            ).toString(),
            Headers.EMPTY,
            new Content.From(
                Flowable.fromArray(ByteBuffer.wrap(data)).flatMap(
                    buffer -> new Splitting(buffer, (random.nextInt(9) + 1) * 512).publisher()
                ).delay(random.nextInt(1_000), TimeUnit.MILLISECONDS)
            )
        ).send(
            (status, headers, publisher) -> {
                MatcherAssert.assertThat(status, new IsEqual<>(RsStatus.OK));
                latch.countDown();
                return CompletableFuture.allOf();
            }
        );
        MatcherAssert.assertThat("Response was not received", latch.await(4, TimeUnit.MINUTES));
        final ByteBuffer res = ByteBuffer.allocate(large);
        ((TestListener) listener).buffers.forEach(item -> res.put(item.position(0)));
        MatcherAssert.assertThat(res.array(), new IsEqual<>(data));
    }

    /**
     * Test listener.
     * @since 0.3
     */
    @SuppressWarnings("PMD.OnlyOneReturn")
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

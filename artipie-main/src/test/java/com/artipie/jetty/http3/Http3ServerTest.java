/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.jetty.http3;

import com.artipie.asto.Content;
import com.artipie.asto.Splitting;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.nuget.RandomFreePort;
import io.reactivex.Flowable;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http3.api.Session;
import org.eclipse.jetty.http3.api.Stream;
import org.eclipse.jetty.http3.client.HTTP3Client;
import org.eclipse.jetty.http3.frames.DataFrame;
import org.eclipse.jetty.http3.frames.HeadersFrame;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Test for {@link Http3Server}.
 */
class Http3ServerTest {

    /**
     * Test header name with request method.
     */
    private static final String RQ_METHOD = "rq_method";

    /**
     * Some test small data chunk.
     */
    private static final byte[] SMALL_DATA = "abc123".getBytes();

    /**
     * Test data size.
     */
    private static final int SIZE = 1024 * 1024;

    private Http3Server server;

    private HTTP3Client client;

    private int port;

    private Session.Client session;

    @BeforeEach
    void init() throws Exception {
        this.port = new RandomFreePort().value();
        final SslContextFactory.Server sslserver = new SslContextFactory.Server();
        sslserver.setKeyStoreType("jks");
        sslserver.setKeyStorePath("src/test/resources/ssl/keystore.jks");
        sslserver.setKeyStorePassword("secret");
        this.server = new Http3Server(new TestSlice(), this.port, sslserver);
        this.server.start();
        this.client = new HTTP3Client();
        this.client.getHTTP3Configuration().setStreamIdleTimeout(15_000);
        final SslContextFactory.Client ssl = new SslContextFactory.Client();
        ssl.setTrustAll(true);
        this.client.getClientConnector().setSslContextFactory(ssl);
        this.client.start();
        this.session = this.client.connect(
            new InetSocketAddress("localhost", this.port), new Session.Client.Listener() { }
        ).get();
    }

    @AfterEach
    void stop() throws Exception {
        this.client.stop();
        this.server.stop();
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "DELETE"})
    void sendsRequestsAndReceivesResponseWithNoData(final String method) throws ExecutionException,
        InterruptedException, TimeoutException {
        final CountDownLatch count = new CountDownLatch(1);
        this.session.newRequest(
            new HeadersFrame(
                new MetaData.Request(
                    method, HttpURI.from(String.format("http://localhost:%d/no_data", this.port)),
                    HttpVersion.HTTP_3, HttpFields.from()
                ), true
            ),
            new Stream.Client.Listener() {
                @Override
                public void onResponse(final Stream.Client stream, final HeadersFrame frame) {
                    final MetaData meta = frame.getMetaData();
                    final MetaData.Response response = (MetaData.Response) meta;
                    MatcherAssert.assertThat(
                        response.getHttpFields().get(Http3ServerTest.RQ_METHOD),
                        new IsEqual<>(method)
                    );
                    count.countDown();
                }
            }
        ).get(5, TimeUnit.SECONDS);
        MatcherAssert.assertThat("Response was not received", count.await(5, TimeUnit.SECONDS));
    }

    @Test
    void getWithSmallResponseData() throws ExecutionException,
        InterruptedException, TimeoutException {
        final MetaData.Request request = new MetaData.Request(
            "GET", HttpURI.from(String.format("http://localhost:%d/small_data", this.port)),
            HttpVersion.HTTP_3, HttpFields.from()
        );
        final StreamTestListener listener = new StreamTestListener(Http3ServerTest.SMALL_DATA.length);
        this.session.newRequest(new HeadersFrame(request, true), listener)
            .get(5, TimeUnit.SECONDS);
        MatcherAssert.assertThat("Response was not received", listener.awaitResponse(5));
        final boolean dataReceived = listener.awaitData(5);
        MatcherAssert.assertThat(
            "Error: response completion timeout. Currently received bytes: %s".formatted(listener.received()),
            dataReceived
        );
        listener.assertDataMatch(Http3ServerTest.SMALL_DATA);
    }

    @Test
    void getWithChunkedResponseData() throws ExecutionException,
        InterruptedException, TimeoutException {
        final MetaData.Request request = new MetaData.Request(
            "GET", HttpURI.from(String.format("http://localhost:%d/random_chunks", this.port)),
            HttpVersion.HTTP_3, HttpFields.from()
        );
        final StreamTestListener listener = new StreamTestListener(Http3ServerTest.SIZE);
        this.session.newRequest(new HeadersFrame(request, true), listener)
            .get(5, TimeUnit.SECONDS);
        MatcherAssert.assertThat("Response was not received", listener.awaitResponse(5));
        final boolean dataReceived = listener.awaitData(60);
        MatcherAssert.assertThat(
            "Error: response completion timeout. Currently received bytes: %s".formatted(listener.received()),
            dataReceived
        );
        MatcherAssert.assertThat(listener.received(), new IsEqual<>(Http3ServerTest.SIZE));
    }

    @Test
    void putWithRequestDataResponse() throws ExecutionException, InterruptedException,
        TimeoutException {
        final int size = 964;
        final MetaData.Request request = new MetaData.Request(
            "PUT", HttpURI.from(String.format("http://localhost:%d/return_back", this.port)),
            HttpVersion.HTTP_3,
            HttpFields.build()
        );
        final StreamTestListener listener = new StreamTestListener(size * 2);
        final byte[] data = new byte[size];
        new Random().nextBytes(data);
        this.session.newRequest(new HeadersFrame(request, false), listener)
            .thenCompose(cl -> cl.data(new DataFrame(ByteBuffer.wrap(data), false)))
            .thenCompose(cl -> cl.data(new DataFrame(ByteBuffer.wrap(data), true)))
            .get(5, TimeUnit.SECONDS);
        MatcherAssert.assertThat("Response was not received", listener.awaitResponse(10));
        final boolean dataReceived = listener.awaitData(10);
        MatcherAssert.assertThat(
            "Error: response completion timeout. Currently received bytes: %s".formatted(listener.received()),
            dataReceived
        );
        final ByteBuffer copy = ByteBuffer.allocate(size * 2);
        copy.put(data);
        copy.put(data);
        listener.assertDataMatch(copy.array());
    }

    /**
     * Slice for tests.
     */
    static final class TestSlice implements Slice {

        @Override
        public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
            if (line.toString().contains("no_data")) {
                return ResponseBuilder.ok()
                    .header( Http3ServerTest.RQ_METHOD, line.method().value())
                    .completedFuture();
            }
            if (line.toString().contains("small_data")) {
                return ResponseBuilder.ok()
                    .body(Http3ServerTest.SMALL_DATA)
                    .completedFuture();
            }
            if (line.toString().contains("random_chunks")) {
                final Random random = new Random();
                final byte[] data = new byte[Http3ServerTest.SIZE];
                random.nextBytes(data);
                return ResponseBuilder.ok().body(
                    new Content.From(
                        Flowable.fromArray(ByteBuffer.wrap(data))
                            .flatMap(
                                buffer -> new Splitting(
                                    buffer, (random.nextInt(9) + 1) * 1024
                                ).publisher()
                            )
                            .delay(random.nextInt(5_000), TimeUnit.MILLISECONDS)
                    )
                ).completedFuture();
            }
            if (line.toString().contains("return_back")) {
                return ResponseBuilder.ok().body(body).completedFuture();
            }
            return ResponseBuilder.notFound().completedFuture();
        }
    }

    /**
     * Client-side listener for testing http3 server responses.
     */
    private static final class StreamTestListener implements Stream.Client.Listener {

        final CountDownLatch responseLatch;

        final CountDownLatch dataAvailableLatch;

        final ByteBuffer buffer;

        StreamTestListener(final int length) {
            this.responseLatch = new CountDownLatch(1);
            this.dataAvailableLatch = new CountDownLatch(1);
            this.buffer = ByteBuffer.allocate(length);
        }

        public boolean awaitResponse(final int seconds) throws InterruptedException {
            return this.responseLatch.await(seconds, TimeUnit.SECONDS);
        }

        public boolean awaitData(final int seconds) throws InterruptedException {
            return this.dataAvailableLatch.await(seconds, TimeUnit.SECONDS);
        }

        public int received() {
            return this.buffer.position();
        }

        public void assertDataMatch(final byte[] copy) {
            Assertions.assertArrayEquals(copy, this.buffer.array());
        }

        @Override
        public void onResponse(final Stream.Client stream, final HeadersFrame frame) {
            responseLatch.countDown();
            stream.demand();
        }

        @Override
        public void onDataAvailable(final Stream.Client stream) {
            final Stream.Data data = stream.readData();
            if (data == null)
            {
                stream.demand();
            } else {
                buffer.put(data.getByteBuffer());
                data.release();
                if (data.isLast()) {
                    dataAvailableLatch.countDown();
                } else {
                    stream.demand();
                }
            }
        }
    }
}

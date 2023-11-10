/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.jetty.http3;

import com.artipie.asto.Content;
import com.artipie.asto.Splitting;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.nuget.RandomFreePort;
import io.reactivex.Flowable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;

/**
 * Test for {@link Http3Server}.
 * @since 0.31
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle AnonInnerLengthCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
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

    /**
     * Server.
     */
    private Http3Server server;

    /**
     * Client.
     */
    private HTTP3Client client;

    /**
     * Port.
     */
    private int port;

    /**
     * Client session.
     */
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
        final CountDownLatch rlatch = new CountDownLatch(1);
        final CountDownLatch dlatch = new CountDownLatch(1);
        final ByteBuffer resp = ByteBuffer.allocate(Http3ServerTest.SMALL_DATA.length);
        this.session.newRequest(
            new HeadersFrame(request, true),
            new Stream.Client.Listener() {

                @Override
                public void onResponse(final Stream.Client stream, final HeadersFrame frame) {
                    rlatch.countDown();
                    stream.demand();
                }

                @Override
                public void onDataAvailable(final Stream.Client stream) {
                    final Stream.Data data = stream.readData();
                    if (data != null) {
                        resp.put(data.getByteBuffer());
                        data.release();
                        if (data.isLast()) {
                            dlatch.countDown();
                        }
                    }
                    stream.demand();
                }
            }
        ).get(5, TimeUnit.SECONDS);
        MatcherAssert.assertThat("Response was not received", rlatch.await(5, TimeUnit.SECONDS));
        MatcherAssert.assertThat("Data were not received", dlatch.await(5, TimeUnit.SECONDS));
        MatcherAssert.assertThat(resp.array(), new IsEqual<byte[]>(Http3ServerTest.SMALL_DATA));
    }

    @Test
    void getWithChunkedResponseData() throws ExecutionException,
        InterruptedException, TimeoutException {
        final MetaData.Request request = new MetaData.Request(
            "GET", HttpURI.from(String.format("http://localhost:%d/random_chunks", this.port)),
            HttpVersion.HTTP_3, HttpFields.from()
        );
        final CountDownLatch rlatch = new CountDownLatch(1);
        final CountDownLatch dlatch = new CountDownLatch(1);
        final ByteBuffer resp = ByteBuffer.allocate(Http3ServerTest.SIZE);
        this.session.newRequest(
            new HeadersFrame(request, true),
            new Stream.Client.Listener() {
                @Override
                public void onResponse(final Stream.Client stream, final HeadersFrame frame) {
                    rlatch.countDown();
                    stream.demand();
                }

                @Override
                public void onDataAvailable(final Stream.Client stream) {
                    final Stream.Data data = stream.readData();
                    if (data != null) {
                        resp.put(data.getByteBuffer());
                        data.release();
                        if (data.isLast()) {
                            dlatch.countDown();
                        }
                    }
                    stream.demand();
                }
            }
        ).get(5, TimeUnit.SECONDS);
        MatcherAssert.assertThat("Response was not received", rlatch.await(5, TimeUnit.SECONDS));
        MatcherAssert.assertThat("Data were not received", dlatch.await(60, TimeUnit.SECONDS));
        MatcherAssert.assertThat(resp.position(), new IsEqual<>(Http3ServerTest.SIZE));
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
        final CountDownLatch rlatch = new CountDownLatch(1);
        final CountDownLatch dlatch = new CountDownLatch(1);
        final byte[] data = new byte[size];
        final ByteBuffer resp = ByteBuffer.allocate(size * 2);
        new Random().nextBytes(data);
        this.session.newRequest(
            new HeadersFrame(request, false),
            new Stream.Client.Listener() {
                @Override
                public void onResponse(final Stream.Client stream, final HeadersFrame frame) {
                    rlatch.countDown();
                    stream.demand();
                }

                @Override
                public void onDataAvailable(final Stream.Client stream) {
                    final Stream.Data data = stream.readData();
                    if (data != null) {
                        resp.put(data.getByteBuffer());
                        data.release();
                        if (data.isLast()) {
                            dlatch.countDown();
                        }
                    }
                    stream.demand();
                }
            }
        ).thenCompose(cl -> cl.data(new DataFrame(ByteBuffer.wrap(data), false)))
            .thenCompose(cl -> cl.data(new DataFrame(ByteBuffer.wrap(data), true)))
            .get(5, TimeUnit.SECONDS);
        MatcherAssert.assertThat("Response was not received", rlatch.await(10, TimeUnit.SECONDS));
        MatcherAssert.assertThat("Data were not received", dlatch.await(60, TimeUnit.SECONDS));
        final ByteBuffer copy = ByteBuffer.allocate(size * 2);
        copy.put(data);
        copy.put(data);
        MatcherAssert.assertThat(resp.array(), new IsEqual<>(copy.array()));
    }

    /**
     * Slice for tests.
     * @since 0.31
     */
    static final class TestSlice implements Slice {

        @Override
        public Response response(
            final String line, final Iterable<Map.Entry<String, String>> headers,
            final Publisher<ByteBuffer> body
        ) {
            final Response res;
            if (line.contains("no_data")) {
                res = new RsWithHeaders(
                    new RsWithStatus(RsStatus.OK),
                    new Header(
                        Http3ServerTest.RQ_METHOD, new RequestLineFrom(line).method().value()
                    )
                );
            } else if (line.contains("small_data")) {
                res = new RsWithBody(new RsWithStatus(RsStatus.OK), Http3ServerTest.SMALL_DATA);
            } else if (line.contains("random_chunks")) {
                final Random random = new Random();
                final byte[] data = new byte[Http3ServerTest.SIZE];
                random.nextBytes(data);
                res = new RsWithBody(
                    new Content.From(
                        Flowable.fromArray(ByteBuffer.wrap(data))
                            .flatMap(
                                buffer -> new Splitting(
                                    buffer, (random.nextInt(9) + 1) * 1024
                                ).publisher()
                            )
                            .delay(random.nextInt(5_000), TimeUnit.MILLISECONDS)
                    )
                );
            } else if (line.contains("return_back")) {
                res = new RsWithBody(new RsWithStatus(RsStatus.OK), body);
            } else {
                res = StandardRs.NOT_FOUND;
            }
            return res;
        }
    }

}

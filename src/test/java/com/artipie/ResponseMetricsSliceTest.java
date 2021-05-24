/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.headers.Authorization;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.metrics.memory.InMemoryMetrics;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ResponseMetricsSlice}.
 *
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("unchecked")
class ResponseMetricsSliceTest {

    /**
     * Metrics collected in tests.
     */
    private InMemoryMetrics metrics;

    @BeforeEach
    public void setUp() {
        this.metrics = new InMemoryMetrics();
    }

    @Test
    public void shouldReportSuccessResponse() {
        this.send(RqMethod.GET, new RsWithStatus(RsStatus.OK));
        this.send(RqMethod.GET, new RsWithStatus(RsStatus.OK));
        MatcherAssert.assertThat(
            this.metrics.counter("get.success").value(),
            new IsEqual<>(2L)
        );
    }

    @Test
    public void shouldReportInternalErrorResponse() {
        this.send(RqMethod.POST, new RsWithStatus(RsStatus.INTERNAL_ERROR));
        this.send(RqMethod.POST, new RsWithStatus(RsStatus.INTERNAL_ERROR));
        MatcherAssert.assertThat(
            this.metrics.counter("post.error").value(),
            new IsEqual<>(2L)
        );
    }

    @Test
    public void shouldReportNotFoundResponse() {
        this.send(RqMethod.HEAD, new RsWithStatus(RsStatus.NOT_FOUND));
        this.send(RqMethod.HEAD, new RsWithStatus(RsStatus.NOT_FOUND));
        MatcherAssert.assertThat(
            this.metrics.counter("head.error").value(),
            new IsEqual<>(2L)
        );
    }

    @Test
    public void shouldReportNoAuthResponse() {
        this.send(RqMethod.PUT, new RsWithStatus(RsStatus.UNAUTHORIZED));
        this.send(RqMethod.PUT, new RsWithStatus(RsStatus.UNAUTHORIZED));
        MatcherAssert.assertThat(
            this.metrics.counter("put.error.no-auth").value(),
            new IsEqual<>(2L)
        );
    }

    @Test
    public void shouldReportBadAuthResponse() {
        this.send(
            RqMethod.DELETE,
            new Headers.From(new Authorization("some value")),
            new RsWithStatus(RsStatus.UNAUTHORIZED)
        );
        this.send(
            RqMethod.DELETE,
            new Headers.From(new Authorization("another value")),
            new RsWithStatus(RsStatus.UNAUTHORIZED)
        );
        MatcherAssert.assertThat(
            this.metrics.counter("delete.error.bad-auth").value(),
            new IsEqual<>(2L)
        );
    }

    @Test
    void shouldForwardRequestUnmodified() {
        final Header header = new Header("header1", "value1");
        final byte[] body = "some code".getBytes();
        final RsStatus status = RsStatus.CREATED;
        MatcherAssert.assertThat(
            new ResponseMetricsSlice(
                (rsline, rsheaders, rsbody) -> new RsFull(
                    status,
                    rsheaders,
                    rsbody
                ),
                this.metrics
            ).response(
                new RequestLine(RqMethod.POST, "/some_upload.war").toString(),
                new Headers.From(header), Flowable.just(ByteBuffer.wrap(body))
            ),
            new ResponseMatcher(status, body, header)
        );
    }

    @Test
    void shouldForwardResponseUnmodified() {
        final Header rsheader = new Header("header2", "value2");
        final byte[] body = "piece of code".getBytes();
        final RsStatus rsstatus = RsStatus.CREATED;
        final Response response = new ResponseMetricsSlice(
            (rsline, rsheaders, rsbody) -> new RsFull(
                rsstatus,
                new Headers.From(rsheader),
                Flowable.just(ByteBuffer.wrap(body))
            ),
            this.metrics
        ).response(new RequestLine(RqMethod.PUT, "/").toString(), Headers.EMPTY, Flowable.empty());
        MatcherAssert.assertThat(
            response,
            new ResponseMatcher(rsstatus, body, rsheader)
        );
    }

    private void send(final RqMethod method, final Response response) {
        this.send(method, Headers.EMPTY, response);
    }

    private void send(final RqMethod method, final Headers headers, final Response response) {
        new ResponseMetricsSlice(
            (rqline, rqheaders, rqbody) -> response,
            this.metrics
        ).response(
            new RequestLine(method.value(), "/file.txt").toString(),
            headers,
            Flowable.empty()
        ).send(
            (rsstatus, rsheaders, rsbody) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
    }
}

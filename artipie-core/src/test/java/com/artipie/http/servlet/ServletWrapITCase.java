/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.servlet;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.misc.RandomFreePort;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rq.RqParams;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.rs.common.RsText;
import com.artipie.http.slice.SliceSimple;
import java.io.Serial;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

/**
 * Integration test for servlet slice wrapper.
 * @since 0.19
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledForJreRange(min = JRE.JAVA_11, disabledReason = "HTTP client is not supported prior JRE_11")
final class ServletWrapITCase {

    /**
     * Jetty server.
     */
    private Server server;

    /**
     * Request builder.
     */
    private HttpRequest.Builder req;

    @BeforeEach
    void setUp() throws Exception {
        final int port = new RandomFreePort().get();
        this.server = new Server(port);
        this.req = HttpRequest.newBuilder(URI.create(String.format("http://localhost:%d", port)));
    }

    @AfterEach
    void tearDown() throws Exception {
        this.server.stop();
    }

    @Test
    void simpleSliceTest() throws Exception {
        final String text = "Hello servlet";
        this.start(new SliceSimple(new RsText(text)));
        final String body = HttpClient.newHttpClient().send(
            this.req.copy().GET().build(),
            HttpResponse.BodyHandlers.ofString()
        ).body();
        MatcherAssert.assertThat(body, new IsEqual<>(text));
    }

    @Test
    void echoSliceTest() throws Exception {
        this.start((line, headers, body) -> new RsWithBody(body));
        final String test = "Ping";
        final String body = HttpClient.newHttpClient().send(
            this.req.copy().PUT(HttpRequest.BodyPublishers.ofString(test)).build(),
            HttpResponse.BodyHandlers.ofString()
        ).body();
        MatcherAssert.assertThat(body, new IsEqual<>(test));
    }

    @Test
    void parsesHeaders() throws Exception {
        this.start(
            (line, headers, body) -> new RsWithHeaders(
                StandardRs.OK,
                new Headers.From("RsHeader", new RqHeaders(headers, "RqHeader").get(0))
            )
        );
        final String value = "some-header";
        final List<String> rsh = HttpClient.newHttpClient().send(
            this.req.copy().GET().header("RqHeader", value).build(),
            HttpResponse.BodyHandlers.discarding()
        ).headers().allValues("RsHeader");
        MatcherAssert.assertThat(
            rsh, Matchers.contains(value)
        );
    }

    @Test
    void returnsStatusCode() throws Exception {
        this.start(new SliceSimple(new RsWithStatus(RsStatus.NO_CONTENT)));
        final int status = HttpClient.newHttpClient().send(
            this.req.copy().GET().build(), HttpResponse.BodyHandlers.discarding()
        ).statusCode();
        // @checkstyle MagicNumberCheck (1 line)
        MatcherAssert.assertThat(status, new IsEqual<>(204));
    }

    @Test
    void echoNoContent() throws Exception {
        this.start((line, headers, body) -> new RsWithBody(body));
        final byte[] body = HttpClient.newHttpClient().send(
            this.req.copy().PUT(HttpRequest.BodyPublishers.noBody()).build(),
            HttpResponse.BodyHandlers.ofByteArray()
        ).body();
        MatcherAssert.assertThat(body.length, new IsEqual<>(0));
    }

    @Test
    void internalErrorOnException() throws Exception {
        final String msg = "Failure123!";
        this.start(
            (line, headers, body) -> {
                throw new IllegalStateException(msg);
            }
        );
        final HttpResponse<String> rsp = HttpClient.newHttpClient().send(
            this.req.copy().GET().build(), HttpResponse.BodyHandlers.ofString()
        );
        MatcherAssert.assertThat("Status is not 500", rsp.statusCode(), new IsEqual<>(500));
        MatcherAssert.assertThat(
            "Body doesn't contain exception message", rsp.body(), new StringContains(msg)
        );
    }

    @Test
    void echoQueryParams() throws Exception {
        this.start(
            (line, header, body) -> new RsWithBody(
                StandardRs.OK,
                new Content.From(
                    new RqParams(
                        new RequestLineFrom(line).uri().getQuery()
                    ).value("foo").orElse("none").getBytes()
                )
            )
        );
        final String param = "? my & param %";
        final String echo = HttpClient.newHttpClient().send(
            this.req.copy().uri(
                new URIBuilder(this.req.build().uri())
                    .addParameter("first", "1&foo=bar&foo=baz")
                    .addParameter("foo", param)
                    .addParameter("bar", "3")
                    .build()
            ).build(),
            HttpResponse.BodyHandlers.ofString()
        ).body();
        MatcherAssert.assertThat(echo, new IsEqual<>(param));
    }

    /**
     * Start Jetty server with slice back-end.
     * @param slice Back-end
     * @throws Exception on server error
     */
    private void start(final Slice slice) throws Exception {
        final ServletContextHandler context = new ServletContextHandler();
        final ServletHolder holder = new ServletHolder(new SliceServlet(slice));
        holder.setAsyncSupported(true);
        context.addServlet(holder, "/");
        this.server.setHandler(context);
        this.server.start();
    }

    /**
     * Servlet implementation with slice back-end.
     * @since 0.19
     */
    private static final class SliceServlet extends GenericServlet {

        @Serial
        private static final long serialVersionUID = 0L;

        /**
         * Slice back-end.
         */
        transient private final Slice target;

        /**
         * New servlet for slice.
         * @param target Slice
         */
        SliceServlet(final Slice target) {
            this.target = target;
        }

        @Override
        public void service(final ServletRequest req,  final ServletResponse rsp) {
            new ServletSliceWrap(this.target).handle(req.startAsync());
        }
    }
}

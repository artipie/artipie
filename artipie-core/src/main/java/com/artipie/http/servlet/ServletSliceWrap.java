/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */

package com.artipie.http.servlet;

import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLine;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.cqfn.rio.Buffers;
import org.cqfn.rio.stream.ReactiveInputStream;

/**
 * Slice wrapper for using in servlet API.
 * @since 0.18
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class ServletSliceWrap {

    /**
     * Target slice.
     */
    private final Slice target;

    /**
     * Wraps {@link Slice} to provide methods for servlet API.
     * @param target Slice
     */
    public ServletSliceWrap(final Slice target) {
        this.target = target;
    }

    /**
     * Handler with async context.
     * @param ctx Servlet async context
     */
    public void handle(final AsyncContext ctx) {
        final HttpServletResponse rsp = (HttpServletResponse) ctx.getResponse();
        this.handle((HttpServletRequest) ctx.getRequest(), rsp)
            .handle(
                (success, error) -> {
                    if (error != null) {
                        Logger.error(this, "Failed to process async request: %[exception]s", error);
                        rsp.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                        try {
                            final PrintWriter writer = rsp.getWriter();
                            writer.println(error.getMessage());
                            error.printStackTrace(writer);
                        } catch (final IOException iex) {
                            Logger.error(this, "Failed to send 500 error: %[exception]s", iex);
                        }
                    }
                    ctx.complete();
                    return success;
                }
            );
    }

    /**
     * Handle servlet request.
     * @param req Servlet request
     * @param rsp Servlet response
     * @return Future
     * @checkstyle ReturnCountCheck (10 lines)
     * @checkstyle IllegalCatchCheck (30 lines)
     */
    @SuppressWarnings({"PMD.OnlyOneReturn", "PMD.AvoidCatchingGenericException"})
    public CompletionStage<Void> handle(final HttpServletRequest req,
        final HttpServletResponse rsp) {
        try {
            final URI uri = new URIBuilder(req.getRequestURI())
                .setCustomQuery(req.getQueryString()).build();
            return this.target.response(
                new RequestLine(
                    req.getMethod(),
                    uri.toASCIIString(),
                    req.getProtocol()
                ).toString(),
                ServletSliceWrap.headers(req),
                new ReactiveInputStream(req.getInputStream()).read(Buffers.Standard.K8)
            ).send(new ServletConnection(rsp));
        } catch (final IOException iex) {
            return ServletSliceWrap.failedStage("Servet IO error", iex);
        } catch (final URISyntaxException err) {
            return ServletSliceWrap.failedStage("Invalid request URI", err);
        } catch (final Exception exx) {
            return ServletSliceWrap.failedStage("Unexpected servlet exception", exx);
        }
    }

    /**
     * Artipie request headers from servlet request.
     * @param req Servlet request
     * @return Artipie headers
     */
    private static Headers headers(final HttpServletRequest req) {
        return new Headers.From(
            Collections.list(req.getHeaderNames()).stream().flatMap(
                name -> Collections.list(req.getHeaders(name)).stream()
                    .map(val -> new Header(name, val))
            ).collect(Collectors.toList())
        );
    }

    /**
     * Convert error to failed stage.
     * @param msg Error message
     * @param err Error exception
     * @return Completion stage
     */
    private static CompletionStage<Void> failedStage(final String msg, final Throwable err) {
        final CompletableFuture<Void> failure = new CompletableFuture<>();
        failure.completeExceptionally(new CompletionException(msg, err));
        return failure;
    }
}

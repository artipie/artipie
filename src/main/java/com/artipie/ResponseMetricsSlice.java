/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.http.ArtipieHttpException;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.headers.Authorization;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.RsStatus;
import com.artipie.metrics.Metrics;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Slice reporting response metrics.
 * Requests and responses are forwarded to origin {@link Slice}.
 * Returned response reported to metrics as success or error if it's status code is 4xx or 5xx.
 *
 * @since 0.9
 */
public final class ResponseMetricsSlice implements Slice {

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Metrics for reporting.
     */
    private final Metrics metrics;

    /**
     * Ctor.
     *
     * @param origin Origin slice.
     * @param metrics Metrics for reporting.
     */
    public ResponseMetricsSlice(final Slice origin, final Metrics metrics) {
        this.origin = origin;
        this.metrics = metrics;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public Response response(
        final String rqline,
        final Iterable<Map.Entry<String, String>> rqheaders,
        final Publisher<ByteBuffer> rqbody) {
        return connection -> {
            try {
                return this.origin.response(rqline, rqheaders, rqbody).send(
                    (rsstatus, rsheaders, rsbody) -> {
                        this.report(rqline, rqheaders, rsstatus);
                        return connection.accept(rsstatus, rsheaders, rsbody);
                    }
                );
            // @checkstyle IllegalCatchCheck (1 line)
            } catch (final Exception exc) {
                this.report(rqline, rqheaders, RsStatus.INTERNAL_ERROR);
                throw new ArtipieHttpException(RsStatus.INTERNAL_ERROR, exc);
            }
        };
    }

    /**
     * Report response to metrics.
     *
     * @param rqline Request line.
     * @param rqheaders Request headers.
     * @param rsstatus Response status.
     */
    private void report(
        final String rqline,
        final Iterable<Map.Entry<String, String>> rqheaders,
        final RsStatus rsstatus
    ) {
        if (rsstatus.error()) {
            switch (rsstatus) {
                case UNAUTHORIZED:
                    this.reportUnauthorized(rqline, rqheaders);
                    break;
                case INTERNAL_ERROR:
                    this.report(rqline, "error.internal");
                    break;
                default:
                    this.report(rqline, "error");
                    break;
            }
        } else {
            this.report(rqline, "success");
        }
    }

    /**
     * Report unauthorized response to metrics.
     *
     * @param rqline Request line.
     * @param rqheaders Request headers.
     */
    private void reportUnauthorized(
        final String rqline,
        final Iterable<Map.Entry<String, String>> rqheaders
    ) {
        if (new RqHeaders(rqheaders, Authorization.NAME).isEmpty()) {
            this.report(rqline, "error.no-auth");
        } else {
            this.report(rqline, "error.bad-auth");
        }
    }

    /**
     * Report response to metrics.
     *
     * @param rqline Request line.
     * @param result Response result string.
     */
    private void report(final String rqline, final String result) {
        final String name = String.format(
            "%s.%s",
            new RequestLineFrom(rqline).method().value().toLowerCase(Locale.getDefault()),
            result
        );
        this.metrics.counter(name).inc();
    }
}

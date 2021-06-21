/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.MeasuredSlice;
import com.artipie.ResponseMetricsSlice;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.metrics.Metrics;
import com.artipie.metrics.PrefixedMetrics;
import java.util.logging.Level;

/**
 * Slice is base for any slice served by Artipie.
 * It is designed to gather request & response metrics, perform logging, handle errors at top level.
 * With all that functionality provided request are forwarded to origin slice
 * and response is given back to caller.
 *
 * @since 0.11
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class BaseSlice extends Slice.Wrap {

    /**
     * Ctor.
     *
     * @param metrics Metrics.
     * @param origin Origin slice.
     */
    public BaseSlice(final Metrics metrics, final Slice origin) {
        super(
            new TrafficMetricSlice(
                new ResponseMetricsSlice(
                    new SafeSlice(
                        new MeasuredSlice(
                            new LoggingSlice(
                                Level.INFO,
                                origin
                            )
                        )
                    ),
                    new PrefixedMetrics(metrics, "http.response.")
                ),
                new PrefixedMetrics(metrics, "http.")
            )
        );
    }
}

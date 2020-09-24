/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.http;

import com.artipie.MeasuredSlice;
import com.artipie.ResponseMetricsSlice;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.metrics.Metrics;
import com.artipie.metrics.PrefixedMetrics;
import java.time.Duration;
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
                        new TimeoutSlice(
                            new MeasuredSlice(
                                new LoggingSlice(
                                    Level.INFO,
                                    origin
                                )
                            ),
                            Duration.ofMinutes(1)
                        )
                    ),
                    new PrefixedMetrics(metrics, "http.response.")
                ),
                new PrefixedMetrics(metrics, "http.")
            )
        );
    }
}

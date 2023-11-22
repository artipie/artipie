/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.http.slice.LoggingSlice;
import com.artipie.jfr.JfrSlice;
import com.artipie.micrometer.MicrometerSlice;
import com.artipie.settings.MetricsContext;
import java.util.logging.Level;

/**
 * Slice is base for any slice served by Artipie.
 * It is designed to gather request & response metrics, perform logging, handle errors at top level.
 * With all that functionality provided request are forwarded to origin slice
 * and response is given back to caller.
 *
 * @since 0.11
 */
public final class BaseSlice extends Slice.Wrap {

    /**
     * Ctor.
     *
     * @param mctx Metrics context.
     * @param origin Origin slice.
     */
    public BaseSlice(final MetricsContext mctx, final Slice origin) {
        super(
            BaseSlice.wrapToBaseMetricsSlices(
                mctx, new JfrSlice(
                    new SafeSlice(
                        new LoggingSlice(Level.INFO, origin)
                    )
                )
            )
        );
    }

    /**
     * Wraps slice to metric related slices when {@code Metrics} is defined.
     *
     * @param mctx Metrics context.
     * @param origin Original slice.
     * @return Wrapped slice.
     */
    private static Slice wrapToBaseMetricsSlices(final MetricsContext mctx, final Slice origin) {
        Slice res = origin;
        if (mctx.http()) {
            res = new MicrometerSlice(origin);
        }
        return res;
    }
}

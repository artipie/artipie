/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.http.Slice;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import java.util.regex.Pattern;

/**
 * This slice checks that request Accept-Encoding header contains gzip value,
 * compress output body with gzip and adds {@code Content-Encoding: gzip} header.
 * <p>
 * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Encoding">Headers Docs</a>.
 * @since 1.1
 */
public final class WithGzipSlice extends Slice.Wrap {

    /**
     * Ctor.
     *
     * @param origin Slice.
     */
    public WithGzipSlice(final Slice origin) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new RtRule.ByHeader("Accept-Encoding", Pattern.compile(".*gzip.*")),
                    new GzipSlice(origin)
                ),
                new RtRulePath(RtRule.FALLBACK, origin)
            )
        );
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.composer.http.proxy;

import com.artipie.http.Slice;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.SliceSimple;
import java.nio.charset.StandardCharsets;

/**
 * Slice for obtaining all packages file with empty packages and specified metadata url.
 * @since 0.4
 */
final class EmptyAllPackagesSlice extends Slice.Wrap {
    /**
     * Ctor.
     */
    EmptyAllPackagesSlice() {
        super(
            new SliceSimple(
                new RsWithBody(
                    StandardRs.OK,
                    "{\"packages\":{}, \"metadata-url\":\"/p2/%package%.json\"}"
                        .getBytes(StandardCharsets.UTF_8)
                )
            )
        );
    }
}

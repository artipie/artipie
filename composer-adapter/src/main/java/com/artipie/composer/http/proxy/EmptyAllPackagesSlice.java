/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http.proxy;

import com.artipie.http.Slice;
import com.artipie.http.rs.BaseResponse;
import com.artipie.http.slice.SliceSimple;

/**
 * Slice for obtaining all packages file with empty packages and specified metadata url.
 */
final class EmptyAllPackagesSlice extends Slice.Wrap {
    EmptyAllPackagesSlice() {
        super(
            new SliceSimple(
                () -> BaseResponse.ok()
                    .jsonBody("{\"packages\":{}, \"metadata-url\":\"/p2/%package%.json\"}")
            )
        );
    }
}

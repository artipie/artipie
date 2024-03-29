/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.hex.http;

import com.artipie.http.Slice;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.RsStatus;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DocsSlice}.
 * @since 0.2
 */
class DocsSliceTest {
    /**
     * Docs slice.
     */
    private Slice docslice;

    @BeforeEach
    void init() {
        this.docslice = new DocsSlice();
    }

    @Test
    void responseOk() {
        MatcherAssert.assertThat(
            this.docslice,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.GET, "/decimal/docs")
            )
        );
    }

}

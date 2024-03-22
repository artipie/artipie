/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.BaseResponse;
import com.artipie.http.rs.RsStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Test for {@link WithGzipSlice}.
 */
class WithGzipSliceTest {

    @Test
    void returnsGzipedResponseIfAcceptEncodingIsPassed() throws IOException {
        MatcherAssert.assertThat(
            new WithGzipSlice(new SliceSimple(BaseResponse.ok().textBody("some content to gzip"))),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(GzipSliceTest.gzip("some content to gzip".getBytes(StandardCharsets.UTF_8))),
                    new RsHasHeaders(new ContentLength(20), new Header("Content-Encoding", "gzip"))
                ),
                new RequestLine(RqMethod.GET, "/"),
                Headers.from(new Header("accept-encoding", "gzip")),
                Content.EMPTY
            )
        );
    }

    @Test
    void returnsResponseAsIsIfAcceptEncodingIsNotPassed() {
        final byte[] data = "abc123".getBytes(StandardCharsets.UTF_8);
        final Header hdr = new Header("name", "value");
        MatcherAssert.assertThat(
            new WithGzipSlice(
                new SliceSimple(
                    BaseResponse.created()
                        .header(hdr)
                        .body(data)
                )
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.CREATED),
                    new RsHasBody(data),
                    new RsHasHeaders(new ContentLength(data.length), hdr)
                ),
                new RequestLine(RqMethod.GET, "/")
            )
        );
    }

}

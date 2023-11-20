/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
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
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.StandardRs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link WithGzipSlice}.
 * @since 1.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class WithGzipSliceTest {

    @Test
    void returnsGzipedResponseIfAcceptEncodingIsPassed() throws IOException {
        final byte[] data = "some content to gzip".getBytes(StandardCharsets.UTF_8);
        MatcherAssert.assertThat(
            new WithGzipSlice(new SliceSimple(new RsWithBody(StandardRs.OK, data))),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(GzipSliceTest.gzip(data)),
                    new RsHasHeaders(new ContentLength(20), new Header("Content-Encoding", "gzip"))
                ),
                new RequestLine(RqMethod.GET, "/"),
                new Headers.From(new Header("accept-encoding", "gzip")),
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
                    new RsFull(RsStatus.CREATED, new Headers.From(hdr), new Content.From(data))
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

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.http.Headers;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.StandardRs;
import io.reactivex.Flowable;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link SliceWithHeaders}.
 */
class SliceWithHeadersTest {

    @Test
    void addsHeaders() {
        final String header = "Content-type";
        final String value = "text/plain";
        MatcherAssert.assertThat(
            new SliceWithHeaders(
                new SliceSimple(StandardRs.EMPTY), new Headers.From(header, value)
            ).response(RequestLine.from("GET /some/text HTTP/1.1"), Headers.EMPTY, Flowable.empty()),
            new RsHasHeaders(new Header(header, value))
        );
    }

    @Test
    void addsHeaderToAlreadyExistingHeaders() {
        final String hone = "Keep-alive";
        final String vone = "true";
        final String htwo = "Authorization";
        final String vtwo = "123";
        MatcherAssert.assertThat(
            new SliceWithHeaders(
                new SliceSimple(
                    new RsWithHeaders(StandardRs.EMPTY, hone, vone)
                ), new Headers.From(htwo, vtwo)
            ).response(RequestLine.from("GET /any/text HTTP/1.1"), Headers.EMPTY, Flowable.empty()),
            new RsHasHeaders(
                new Header(hone, vone), new Header(htwo, vtwo)
            )
        );
    }

}

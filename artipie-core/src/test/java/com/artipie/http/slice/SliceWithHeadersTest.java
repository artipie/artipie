/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Headers;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.rq.RequestLine;
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
                new SliceSimple(ResponseBuilder.ok().build()), Headers.from(header, value)
            ).response(RequestLine.from("GET /some/text HTTP/1.1"), Headers.EMPTY, Content.EMPTY),
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
                    ResponseBuilder.ok().header(hone, vone).build()
                ), Headers.from(htwo, vtwo)
            ).response(RequestLine.from("GET /any/text HTTP/1.1"), Headers.EMPTY, Content.EMPTY),
            new RsHasHeaders(
                new Header(hone, vone), new Header(htwo, vtwo)
            )
        );
    }

}

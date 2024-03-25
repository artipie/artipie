/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.rs.RsStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.Collections;

/**
 * Tests for {@link SliceFromResource}.
 */
public class SliceFromResourceTest {

    @Test
    void shouldDelegateGetResponse() {
        final Header header = new Header("Name", "Value");
        final byte[] body = "body".getBytes();
        final Response response = new SliceFromResource(
            new Resource() {
                @Override
                public Response get(final Headers headers) {
                    return ResponseBuilder.ok().headers(headers)
                        .body(body).build();
                }

                @Override
                public Response put(Headers headers, Content body) {
                    throw new UnsupportedOperationException();
                }
            }
        ).response(
            new RequestLine(RqMethod.GET, "/some/path"),
            Headers.from(Collections.singleton(header)),
            Content.EMPTY
        );
        MatcherAssert.assertThat(
            response,
            Matchers.allOf(
                new RsHasStatus(RsStatus.OK),
                new RsHasHeaders(header),
                new RsHasBody(body)
            )
        );
    }

    @Test
    void shouldDelegatePutResponse() {
        final Header header = new Header("X-Name", "Something");
        final byte[] content = "content".getBytes();
        final Response response = new SliceFromResource(
            new Resource() {
                @Override
                public Response get(Headers headers) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Response put(Headers headers, Content body) {
                    return ResponseBuilder.ok().headers(headers)
                        .body(body).build();
                }
            }
        ).response(
            new RequestLine(RqMethod.PUT, "/some/other/path"),
            Headers.from(Collections.singleton(header)),
            new Content.From(content)
        );
        MatcherAssert.assertThat(
            response,
            Matchers.allOf(
                new RsHasStatus(RsStatus.OK),
                new RsHasHeaders(header),
                new RsHasBody(content)
            )
        );
    }
}

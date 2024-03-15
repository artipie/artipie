/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

/**
 * Tests for {@link SliceFromResource}.
 *
 * @since 0.2
 */
public class SliceFromResourceTest {

    @Test
    void shouldDelegateGetResponse() {
        final RsStatus status = RsStatus.OK;
        final Header header = new Header("Name", "Value");
        final byte[] body = "body".getBytes();
        final Response response = new SliceFromResource(
            new Resource() {
                @Override
                public Response get(final Headers headers) {
                    return new RsFull(
                        status,
                        headers,
                        Flowable.just(ByteBuffer.wrap(body))
                    );
                }

                @Override
                public Response put(final Headers headers, final Publisher<ByteBuffer> body) {
                    throw new UnsupportedOperationException();
                }
            }
        ).response(
            new RequestLine(RqMethod.GET, "/some/path"),
            new Headers.From(Collections.singleton(header)),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            response,
            Matchers.allOf(
                new RsHasStatus(status),
                new RsHasHeaders(header),
                new RsHasBody(body)
            )
        );
    }

    @Test
    void shouldDelegatePutResponse() {
        final RsStatus status = RsStatus.OK;
        final Header header = new Header("X-Name", "Something");
        final byte[] content = "content".getBytes();
        final Response response = new SliceFromResource(
            new Resource() {
                @Override
                public Response get(final Headers headers) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Response put(final Headers headers, final Publisher<ByteBuffer> body) {
                    return new RsFull(status, headers, body);
                }
            }
        ).response(
            new RequestLine(RqMethod.PUT, "/some/other/path"),
            new Headers.From(Collections.singleton(header)),
            Flowable.just(ByteBuffer.wrap(content))
        );
        MatcherAssert.assertThat(
            response,
            Matchers.allOf(
                new RsHasStatus(status),
                new RsHasHeaders(header),
                new RsHasBody(content)
            )
        );
    }
}

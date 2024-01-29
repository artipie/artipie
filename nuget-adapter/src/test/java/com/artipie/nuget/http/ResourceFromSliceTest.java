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

/**
 * Tests for {@link ResourceFromSlice}.
 *
 * @since 0.2
 */
final class ResourceFromSliceTest {

    @Test
    void shouldDelegateGetResponse() {
        final RsStatus status = RsStatus.OK;
        final String path = "/some/path";
        final Header header = new Header("Name", "Value");
        final Response response = new ResourceFromSlice(
            path,
            (line, hdrs, body) -> new RsFull(
                status,
                hdrs,
                Flowable.just(ByteBuffer.wrap(line.getBytes()))
            )
        ).get(new Headers.From(Collections.singleton(header)));
        MatcherAssert.assertThat(
            response,
            Matchers.allOf(
                new RsHasStatus(status),
                new RsHasHeaders(header),
                new RsHasBody(
                    new RequestLine(RqMethod.GET, path).toString().getBytes()
                )
            )
        );
    }

    @Test
    void shouldDelegatePutResponse() {
        final RsStatus status = RsStatus.OK;
        final String path = "/some/other/path";
        final Header header = new Header("X-Name", "Something");
        final String content = "body";
        final Response response = new ResourceFromSlice(
            path,
            (line, hdrs, body) -> new RsFull(
                status,
                hdrs,
                Flowable.concat(Flowable.just(ByteBuffer.wrap(line.getBytes())), body)
            )
        ).put(
            new Headers.From(Collections.singleton(header)),
            Flowable.just(ByteBuffer.wrap(content.getBytes()))
        );
        MatcherAssert.assertThat(
            response,
            Matchers.allOf(
                new RsHasStatus(status),
                new RsHasHeaders(header),
                new RsHasBody(
                    String.join(
                        "",
                        new RequestLine(RqMethod.PUT, path).toString(),
                        content
                    ).getBytes()
                )
            )
        );
    }
}

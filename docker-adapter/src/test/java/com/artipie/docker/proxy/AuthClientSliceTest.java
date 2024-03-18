/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.client.auth.AuthClientSlice;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AuthClientSlice}.
 */
class AuthClientSliceTest {

    @Test
    void shouldNotModifyRequestAndResponseIfNoAuthRequired() {
        final RequestLine line = new RequestLine(RqMethod.GET, "/file.txt");
        final Header header = new Header("x-name", "some value");
        final byte[] body = "text".getBytes();
        final RsStatus status = RsStatus.OK;
        final Response response = new AuthClientSlice(
            (rsline, rsheaders, rsbody) -> {
                if (!rsline.equals(line)) {
                    throw new IllegalArgumentException(String.format("Line modified: %s", rsline));
                }
                return new RsFull(status, rsheaders, rsbody);
            },
            Authenticator.ANONYMOUS
        ).response(line, Headers.from(header), Flowable.just(ByteBuffer.wrap(body)));
        MatcherAssert.assertThat(
            response,
            new ResponseMatcher(status, body, header)
        );
    }
}

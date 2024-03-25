/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.http.BaseResponse;
import com.artipie.http.Headers;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.ContentType;
import com.artipie.http.headers.Header;
import com.artipie.http.headers.WwwAuthenticate;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Test case for {@link DockerAuthSlice}.
 */
public final class DockerAuthSliceTest {

    @Test
    void shouldReturnErrorsWhenUnathorized() {
        final Headers headers = Headers.from(
            new WwwAuthenticate("Basic"),
            new Header("X-Something", "Value")
        );
        MatcherAssert.assertThat(
            new DockerAuthSlice(
                (rqline, rqheaders, rqbody) -> BaseResponse.unauthorized().headers(headers)
            ).response(
                new RequestLine(RqMethod.GET, "/"),
                Headers.EMPTY, Content.EMPTY
            ),
            new AllOf<>(
                Arrays.asList(
                    new IsUnauthorizedResponse(),
                    new RsHasHeaders(
                        Headers.from(
                            new WwwAuthenticate("Basic"),
                            new Header("X-Something", "Value"),
                            ContentType.json(),
                            new ContentLength("72")
                        )
                    )
                )
            )
        );
    }

    @Test
    void shouldReturnErrorsWhenForbidden() {
        final Headers headers = Headers.from(
            new WwwAuthenticate("Basic realm=\"123\""),
            new Header("X-Foo", "Bar")
        );
        MatcherAssert.assertThat(
            new DockerAuthSlice(
                (rqline, rqheaders, rqbody) -> BaseResponse.forbidden().headers(headers.copy())
            ).response(
                new RequestLine(RqMethod.GET, "/file.txt"),
                Headers.EMPTY,
                Content.EMPTY
            ),
            new AllOf<>(
                Arrays.asList(
                    new IsDeniedResponse(),
                    new RsHasHeaders(
                        headers.copy()
                            .add(ContentType.json())
                            .add(new ContentLength("85"))
                    )
                )
            )
        );
    }

    @Test
    void shouldNotModifyNormalResponse() {
        final RsStatus status = RsStatus.OK;
        final byte[] body = "data".getBytes();
        MatcherAssert.assertThat(
            new DockerAuthSlice(
                (rqline, rqheaders, rqbody) -> BaseResponse.ok()
                    .header(ContentType.text())
                    .body(body)
            ).response(
                new RequestLine(RqMethod.GET, "/some/path"),
                Headers.EMPTY, Content.EMPTY
            ),
            new ResponseMatcher(
                status, Collections.singleton(ContentType.text()), body
            )
        );
    }
}

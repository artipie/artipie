/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.RsStatus;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.ContentType;
import com.artipie.http.headers.Header;
import com.artipie.http.headers.WwwAuthenticate;
import com.artipie.http.hm.ResponseAssert;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.Test;

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
                (rqline, rqheaders, rqbody) -> ResponseBuilder.unauthorized().headers(headers).completedFuture()
            ).response(
                new RequestLine(RqMethod.GET, "/"),
                Headers.EMPTY, Content.EMPTY
            ).join(),
            new AllOf<>(
                new IsErrorsResponse(RsStatus.UNAUTHORIZED, "UNAUTHORIZED"),
                new RsHasHeaders(
                    new WwwAuthenticate("Basic"),
                    new Header("X-Something", "Value"),
                    ContentType.json(),
                    new ContentLength("72")
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
                (rqline, rqheaders, rqbody) -> ResponseBuilder.forbidden().headers(headers.copy()).completedFuture()
            ).response(
                new RequestLine(RqMethod.GET, "/file.txt"),
                Headers.EMPTY,
                Content.EMPTY
            ).join(),
            new AllOf<>(
                new IsErrorsResponse(RsStatus.FORBIDDEN, "DENIED"),
                new RsHasHeaders(
                    headers.copy()
                        .add(ContentType.json())
                        .add(new ContentLength("85"))
                )
            )
        );
    }

    @Test
    void shouldNotModifyNormalResponse() {
        final RsStatus status = RsStatus.OK;
        final byte[] body = "data".getBytes();
        ResponseAssert.check(
            new DockerAuthSlice(
                (rqline, rqheaders, rqbody) -> ResponseBuilder.ok()
                    .header(ContentType.text())
                    .body(body)
                    .completedFuture()
            ).response(
                new RequestLine(RqMethod.GET, "/some/path"),
                Headers.EMPTY, Content.EMPTY
            ).join(),
            status, body, ContentType.text()
        );
    }
}

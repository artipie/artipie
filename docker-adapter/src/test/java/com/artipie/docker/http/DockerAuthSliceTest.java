/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.Header;
import com.artipie.http.headers.WwwAuthenticate;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link DockerAuthSlice}.
 *
 * @since 0.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class DockerAuthSliceTest {

    @Test
    void shouldReturnErrorsWhenUnathorized() {
        final Headers headers = new Headers.From(
            new WwwAuthenticate("Basic"),
            new Header("X-Something", "Value")
        );
        MatcherAssert.assertThat(
            new DockerAuthSlice(
                (rqline, rqheaders, rqbody) -> new RsWithHeaders(
                    new RsWithStatus(RsStatus.UNAUTHORIZED),
                    new Headers.From(headers)
                )
            ).response(
                new RequestLine(RqMethod.GET, "/").toString(),
                Headers.EMPTY,
                Flowable.empty()
            ),
            new AllOf<>(
                Arrays.asList(
                    new IsUnauthorizedResponse(),
                    new RsHasHeaders(
                        new Headers.From(headers, new JsonContentType(), new ContentLength("72"))
                    )
                )
            )
        );
    }

    @Test
    void shouldReturnErrorsWhenForbidden() {
        final Headers headers = new Headers.From(
            new WwwAuthenticate("Basic realm=\"123\""),
            new Header("X-Foo", "Bar")
        );
        MatcherAssert.assertThat(
            new DockerAuthSlice(
                (rqline, rqheaders, rqbody) -> new RsWithHeaders(
                    new RsWithStatus(RsStatus.FORBIDDEN),
                    new Headers.From(headers)
                )
            ).response(
                new RequestLine(RqMethod.GET, "/file.txt").toString(),
                Headers.EMPTY,
                Content.EMPTY
            ),
            new AllOf<>(
                Arrays.asList(
                    new IsDeniedResponse(),
                    new RsHasHeaders(
                        new Headers.From(headers, new JsonContentType(), new ContentLength("85"))
                    )
                )
            )
        );
    }

    @Test
    void shouldNotModifyNormalResponse() {
        final RsStatus status = RsStatus.OK;
        final Collection<Map.Entry<String, String>> headers = Collections.singleton(
            new Header("Content-Type", "text/plain")
        );
        final byte[] body = "data".getBytes();
        MatcherAssert.assertThat(
            new DockerAuthSlice(
                (rqline, rqheaders, rqbody) -> new RsFull(
                    status,
                    new Headers.From(headers),
                    Flowable.just(ByteBuffer.wrap(body))
                )
            ).response(
                new RequestLine(RqMethod.GET, "/some/path").toString(),
                Headers.EMPTY,
                Flowable.empty()
            ),
            new ResponseMatcher(status, headers, body)
        );
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.ExampleStorage;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DockerSlice}.
 * Manifest HEAD endpoint.
 */
class ManifestEntityHeadTest {

    /**
     * Slice being tested.
     */
    private DockerSlice slice;

    @BeforeEach
    void setUp() {
        this.slice = new DockerSlice(new AstoDocker(new ExampleStorage()));
    }

    @Test
    void shouldRespondOkWhenManifestFoundByTag() {
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(RqMethod.HEAD, "/v2/my-alpine/manifests/1"),
                Headers.from(
                    new Header("Accept", "application/vnd.docker.distribution.manifest.v2+json, application/xml;q=0.9, image/*")
                ),
                Content.EMPTY
            ).join(),
            new ResponseMatcher(
                "sha256:cb8a924afdf0229ef7515d9e5b3024e23b3eb03ddbba287f4a19c6ac90b8d221",
                528
            )
        );
    }

    @Test
    void shouldRespondOkWhenManifestFoundByDigest() {
        final String digest = String.format(
            "%s:%s",
            "sha256",
            "cb8a924afdf0229ef7515d9e5b3024e23b3eb03ddbba287f4a19c6ac90b8d221"
        );
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(
                    RqMethod.HEAD,
                    String.format("/v2/my-alpine/manifests/%s", digest)
                ),
                Headers.from(
                    new Header("Accept", "application/vnd.docker.distribution.manifest.v2+json, application/xml;q=0.9, image/*")
                ),
                Content.EMPTY
            ).join(),
            new ResponseMatcher(digest, 528)
        );
    }

    @Test
    void shouldReturnNotFoundForUnknownTag() {
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(RqMethod.HEAD, "/v2/my-alpine/manifests/2"),
                Headers.from(
                    new Header("Accept", "application/vnd.docker.distribution.manifest.v2+json, application/xml;q=0.9, image/*")
                ),
                Content.EMPTY
            ).join(),
            new IsErrorsResponse(RsStatus.NOT_FOUND, "MANIFEST_UNKNOWN")
        );
    }

    @Test
    void shouldReturnNotFoundForUnknownDigest() {
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(
                    RqMethod.HEAD,
                    String.format(
                        "/v2/my-alpine/manifests/%s",
                        "sha256:0123456789012345678901234567890123456789012345678901234567890123"
                    )),
                Headers.from(
                    new Header("Accept", "application/vnd.docker.distribution.manifest.v2+json, application/xml;q=0.9, image/*")
                ),
                Content.EMPTY
            ).join(),
            new IsErrorsResponse(RsStatus.NOT_FOUND, "MANIFEST_UNKNOWN")
        );
    }

    /**
     * Manifest entity head response matcher.
     * @since 0.3
     */
    private static final class ResponseMatcher extends AllOf<Response> {

        /**
         * Ctor.
         *
         * @param digest Expected `Docker-Content-Digest` header value.
         * @param size Expected `Content-Length` header value.
         */
        ResponseMatcher(final String digest, final long size) {
            super(
                new ListOf<Matcher<? super Response>>(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasHeaders(
                        new Header(
                            "Content-type", "application/vnd.docker.distribution.manifest.v2+json"
                        ),
                        new Header("Docker-Content-Digest", digest),
                        new Header("Content-Length", String.valueOf(size))
                    )
                )
            );
        }

    }

}

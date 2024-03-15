/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Key;
import com.artipie.docker.ExampleStorage;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.http.Response;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DockerSlice}.
 * Manifest GET endpoint.
 */
class ManifestEntityGetTest {

    /**
     * Slice being tested.
     */
    private DockerSlice slice;

    @BeforeEach
    void setUp() {
        this.slice = new DockerSlice(new AstoDocker(new ExampleStorage()));
    }

    @Test
    void shouldReturnManifestByTag() {
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(RqMethod.GET, "/v2/my-alpine/manifests/1"),
                new Headers(),
                Flowable.empty()
            ),
            new ResponseMatcher(
                "sha256:cb8a924afdf0229ef7515d9e5b3024e23b3eb03ddbba287f4a19c6ac90b8d221",
                bytes(
                    new Key.From(
                        "blobs", "sha256", "cb",
                        "cb8a924afdf0229ef7515d9e5b3024e23b3eb03ddbba287f4a19c6ac90b8d221", "data"
                    )
                )
            )
        );
    }

    @Test
    void shouldReturnManifestByDigest() {
        final String hex = "cb8a924afdf0229ef7515d9e5b3024e23b3eb03ddbba287f4a19c6ac90b8d221";
        final String digest = String.format("%s:%s", "sha256", hex);
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(
                    RqMethod.GET,
                    String.format("/v2/my-alpine/manifests/%s", digest)
                ),
                new Headers(),
                Flowable.empty()
            ),
            new ResponseMatcher(
                digest,
                bytes(new Key.From("blobs", "sha256", "cb", hex, "data"))
            )
        );
    }

    @Test
    void shouldReturnNotFoundForUnknownTag() {
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(RqMethod.GET, "/v2/my-alpine/manifests/2"),
                new Headers(),
                Flowable.empty()
            ),
            new IsErrorsResponse(RsStatus.NOT_FOUND, "MANIFEST_UNKNOWN")
        );
    }

    @Test
    void shouldReturnNotFoundForUnknownDigest() {
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine(
                    RqMethod.GET,
                    String.format(
                        "/v2/my-alpine/manifests/%s",
                        "sha256:0123456789012345678901234567890123456789012345678901234567890123"
                    )
                ),
                new Headers(),
                Flowable.empty()
            ),
            new IsErrorsResponse(RsStatus.NOT_FOUND, "MANIFEST_UNKNOWN")
        );
    }

    private static byte[] bytes(final Key key) {
        return new ExampleStorage().value(key).join().asBytes();
    }

    /**
     * Headers set for getting manifest.
     *
     * @since 0.4
     */
    private static class Headers extends com.artipie.http.Headers.Wrap {

        Headers() {
            super(
                new Headers.From(
                    new Header("Accept", "application/vnd.docker.distribution.manifest.v2+json, application/xml;q=0.9, image/webp")
                )
            );
        }
    }

    /**
     * Response matcher.
     * @since 0.3
     */
    private static final class ResponseMatcher extends AllOf<Response> {

        /**
         * Ctor.
         * @param digest Digest
         * @param content Content
         */
        ResponseMatcher(final String digest, final byte[] content) {
            super(
                new ListOf<Matcher<? super Response>>(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasHeaders(
                        new Header("Content-Length", String.valueOf(content.length)),
                        new Header(
                            "Content-Type",
                            "application/vnd.docker.distribution.manifest.v2+json"
                        ),
                        new Header("Docker-Content-Digest", digest)
                    ),
                    new RsHasBody(content)
                )
            );
        }
    }
}

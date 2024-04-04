/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ManifestEntity.Request}.
 */
class ManifestEntityRequestTest {

    @Test
    void shouldReadName() {
        final ManifestEntity.Request request = new ManifestEntity.Request(
            new RequestLine(RqMethod.GET, "/v2/my-repo/manifests/3")
        );
        MatcherAssert.assertThat(request.name(), new IsEqual<>("my-repo"));
    }

    @Test
    void shouldReadReference() {
        final ManifestEntity.Request request = new ManifestEntity.Request(
            new RequestLine(RqMethod.GET, "/v2/my-repo/manifests/sha256:123abc")
        );
        MatcherAssert.assertThat(request.reference().reference(), new IsEqual<>("sha256:123abc"));
    }

    @Test
    void shouldReadCompositeName() {
        final String name = "zero-one/two.three/four_five";
        MatcherAssert.assertThat(
            new ManifestEntity.Request(
                new RequestLine(
                    "HEAD", String.format("/v2/%s/manifests/sha256:234434df", name)
                )
            ).name(),
            new IsEqual<>(name)
        );
    }

}

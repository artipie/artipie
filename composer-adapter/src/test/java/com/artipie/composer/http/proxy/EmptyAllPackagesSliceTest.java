/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http.proxy;

import com.artipie.composer.AllPackages;
import com.artipie.http.Response;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.nio.charset.StandardCharsets;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link EmptyAllPackagesSlice}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class EmptyAllPackagesSliceTest {
    @Test
    void returnsCorrectResponse() {
        final byte[] body = "{\"packages\":{}, \"metadata-url\":\"/p2/%package%.json\"}"
            .getBytes(StandardCharsets.UTF_8);
        MatcherAssert.assertThat(
            new EmptyAllPackagesSlice(),
            new SliceHasResponse(
                new AllOf<>(
                    new ListOf<Matcher<? super Response>>(
                        new RsHasStatus(RsStatus.OK),
                        new RsHasHeaders(new ContentLength(body.length)),
                        new RsHasBody(body)
                    )
                ),
                new RequestLine(RqMethod.GET, new AllPackages().string())
            )
        );
    }
}

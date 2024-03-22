/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.http.BaseResponse;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * Test for {@link SliceOptional}.
 */
class SliceOptionalTest {

    @Test
    void returnsNotFoundWhenAbsent() {
        MatcherAssert.assertThat(
            new SliceOptional<>(
                Optional.empty(),
                Optional::isPresent,
                ignored -> new SliceSimple(BaseResponse.ok())
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/any")
            )
        );
    }

    @Test
    void returnsCreatedWhenConditionIsMet() {
        MatcherAssert.assertThat(
            new SliceOptional<>(
                Optional.of("abc"),
                Optional::isPresent,
                ignored -> new SliceSimple(BaseResponse.noContent())
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NO_CONTENT),
                new RequestLine(RqMethod.GET, "/abc")
            )
        );
    }

    @Test
    void appliesSliceFunction() {
        final String body = "Hello";
        MatcherAssert.assertThat(
            new SliceOptional<>(
                Optional.of(body),
                Optional::isPresent,
                hello -> new SliceSimple(
                    BaseResponse.ok().body(hello.orElseThrow().getBytes())
                )
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(body.getBytes())
                ),
                new RequestLine(RqMethod.GET, "/hello")
            )
        );
    }

}

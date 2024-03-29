/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.IsJson;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.misc.ArtipieProperties;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * Tests for {@link VersionSlice}.
 * @since 0.21
 */
final class VersionSliceTest {
    @Test
    void returnVersionOfApplication() {
        final ArtipieProperties proprts = new ArtipieProperties();
        MatcherAssert.assertThat(
            new VersionSlice(proprts),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(
                        new IsJson(
                            new JsonContains(
                                new JsonHas("version", new JsonValueIs(proprts.version()))
                            )
                        )
                    )
                ),
                new RequestLine(RqMethod.GET, "/.version")
            )
        );
    }
}

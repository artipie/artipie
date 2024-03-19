/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.hm.IsHeader;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RedirectSlice}.
 */
class RedirectSliceTest {

    @Test
    void redirectsToNormalizedName() {
        MatcherAssert.assertThat(
            new RedirectSlice().response(
                new RequestLine(RqMethod.GET, "/one/two/three_four"),
                Headers.EMPTY,
                Content.EMPTY
            ),
            new ResponseMatcher(
                RsStatus.MOVED_PERMANENTLY,
                new IsHeader("Location", "/one/two/three-four")
            )
        );
    }

    @Test
    void redirectsToNormalizedNameWithSlashAtTheEnd() {
        MatcherAssert.assertThat(
            new RedirectSlice().response(
                new RequestLine(RqMethod.GET, "/one/two/three_four/"),
                Headers.EMPTY,
                Content.EMPTY
            ),
            new ResponseMatcher(
                RsStatus.MOVED_PERMANENTLY,
                new IsHeader("Location", "/one/two/three-four")
            )
        );
    }

    @Test
    void redirectsToNormalizedNameWhenFillPathIsPresent() {
        MatcherAssert.assertThat(
            new RedirectSlice().response(
                new RequestLine(RqMethod.GET, "/three/F.O.U.R"),
                Headers.from("X-FullPath", "/one/two/three/F.O.U.R"),
                Content.EMPTY
            ),
            new ResponseMatcher(
                RsStatus.MOVED_PERMANENTLY,
                new IsHeader("Location", "/one/two/three/f-o-u-r")
            )
        );
    }

    @Test
    void normalizesOnlyLastPart() {
        MatcherAssert.assertThat(
            new RedirectSlice().response(
                new RequestLine(RqMethod.GET, "/three/One_Two"),
                Headers.from("X-FullPath", "/One_Two/three/One_Two"),
                Content.EMPTY
            ),
            new ResponseMatcher(
                RsStatus.MOVED_PERMANENTLY,
                new IsHeader("Location", "/One_Two/three/one-two")
            )
        );
    }

}

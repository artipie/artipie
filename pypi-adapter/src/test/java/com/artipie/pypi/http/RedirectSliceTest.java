/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi.http;

import com.artipie.http.Headers;
import com.artipie.http.hm.IsHeader;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RedirectSlice}.
 * @since 0.6
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class RedirectSliceTest {

    @Test
    void redirectsToNormalizedName() {
        MatcherAssert.assertThat(
            new RedirectSlice().response(
                new RequestLine(RqMethod.GET, "/one/two/three_four").toString(),
                Collections.emptyList(),
                Flowable.empty()
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
                new RequestLine(RqMethod.GET, "/one/two/three_four/").toString(),
                Collections.emptyList(),
                Flowable.empty()
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
                new RequestLine(RqMethod.GET, "/three/F.O.U.R").toString(),
                new Headers.From("X-FullPath", "/one/two/three/F.O.U.R"),
                Flowable.empty()
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
                new RequestLine(RqMethod.GET, "/three/One_Two").toString(),
                new Headers.From("X-FullPath", "/One_Two/three/One_Two"),
                Flowable.empty()
            ),
            new ResponseMatcher(
                RsStatus.MOVED_PERMANENTLY,
                new IsHeader("Location", "/One_Two/three/one-two")
            )
        );
    }

}

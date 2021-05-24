/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.asto.Content;
import com.artipie.http.AllRepositoriesSlice;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link AllRepositoriesSlice}.
 *
 * @since 0.11
 */
public final class AllRepositoriesSliceTest {

    @Test
    public void unexistingRepoReturnNotFound() {
        MatcherAssert.assertThat(
            new AllRepositoriesSlice(new Settings.Fake()).response(
                new RequestLine(RqMethod.GET, "/repo/foo").toString(),
                Headers.EMPTY,
                Content.EMPTY
            ),
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"/favicon.ico", "robots.txt"})
    void requestThatNotDirectedToRepoReturnNotFound(final String uri) {
        MatcherAssert.assertThat(
            new AllRepositoriesSlice(new Settings.Fake()).response(
                new RequestLine(RqMethod.GET, uri).toString(),
                Headers.EMPTY,
                Content.EMPTY
            ),
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }
}

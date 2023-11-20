/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */

package com.artipie.http.rs;

import com.artipie.http.hm.RsHasStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link RsWithStatus}.
 * @since 0.1
 */
final class RsWithStatusTest {
    @Test
    void usesStatus() throws Exception {
        final RsStatus status = RsStatus.NOT_FOUND;
        MatcherAssert.assertThat(
            new RsWithStatus(status),
            new RsHasStatus(status)
        );
    }

    @Test
    void toStringRsWithStatus() {
        MatcherAssert.assertThat(
            new RsWithStatus(RsStatus.OK).toString(),
            new IsEqual<>("RsWithStatus{status=OK, origin=EMPTY}")
        );
    }

}

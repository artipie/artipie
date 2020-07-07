/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie;

import java.io.File;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.llorllale.cactoos.matchers.MatcherOf;

/**
 * Test for {@link RpPermissions}.
 * @since 0.2
 * @checkstyle LeftCurlyCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class RpPermissionsTest {

    /**
     * Test configuration file location.
     */
    private static final String CONF_YAML = "src/test/resources/repo-full-config.yml";

    @Test
    void johnCanDownloadDeployAndDelete() {
        final String uname = "john";
        MatcherAssert.assertThat(
            new RpPermissions(new File(RpPermissionsTest.CONF_YAML)),
            new AllOf<RpPermissions>(
                new ListOf<org.hamcrest.Matcher<? super RpPermissions>>(
                    new MatcherOf<>(perm -> { return perm.allowed(uname, "delete"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(uname, "deploy"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(uname, "download"); }),
                    new MatcherOf<>(perm -> !perm.allowed(uname, "install"))
                )
            )
        );
    }

    @Test
    void janeCanDownloadAndDeploy() {
        final String uname = "jane";
        MatcherAssert.assertThat(
            new RpPermissions(new File(RpPermissionsTest.CONF_YAML)),
            new AllOf<RpPermissions>(
                new ListOf<org.hamcrest.Matcher<? super RpPermissions>>(
                    new MatcherOf<>(perm -> { return perm.allowed(uname, "deploy"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(uname, "download"); }),
                    new MatcherOf<>(perm -> !perm.allowed(uname, "install")),
                    new MatcherOf<>(perm -> !perm.allowed(uname, "update"))
                )
            )
        );
    }

    @Test
    void anyoneCanDownload() {
        MatcherAssert.assertThat(
            new RpPermissions(new File(RpPermissionsTest.CONF_YAML)).allowed("anyone", "download"),
            new IsEqual<>(true)
        );
    }

    @Test
    void adminCanDoAnything() {
        final String uname = "admin";
        MatcherAssert.assertThat(
            new RpPermissions(new File(RpPermissionsTest.CONF_YAML)),
            new AllOf<RpPermissions>(
                new ListOf<org.hamcrest.Matcher<? super RpPermissions>>(
                    new MatcherOf<>(perm -> { return perm.allowed(uname, "delete"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(uname, "deploy"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(uname, "download"); }),
                    new MatcherOf<>(perm -> { return perm.allowed(uname, "install"); })
                )
            )
        );
    }

}

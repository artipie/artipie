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
package com.artipie.api.artifactory;

import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.repo.PathPattern;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link GetStorageSlice.Request}.
 *
 * @since 0.11
 */
class GetStorageSliceRequestTest {

    @ParameterizedTest
    @CsvSource({
        "flat,/api/storage/my-lib/foo/bar,my-lib",
        "org,/api/storage/my-company/my-lib/foo/bar,my-company/my-lib"
    })
    void shouldParseRepo(
        final String layout,
        final String path,
        final String repo
    ) {
        final GetStorageSlice.Request request = new GetStorageSlice.Request(
            new PathPattern(layout).pattern(),
            new RequestLine(RqMethod.GET, path).toString()
        );
        MatcherAssert.assertThat(
            request.repo(),
            new IsEqual<>(repo)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "flat,/api/storage/my-lib/foo/bar,foo/bar",
        "org,/api/storage/my-company/my-lib/foo/bar,foo/bar"
    })
    void shouldParseRoot(
        final String layout,
        final String path,
        final String root
    ) {
        final GetStorageSlice.Request request = new GetStorageSlice.Request(
            new PathPattern(layout).pattern(),
            new RequestLine(RqMethod.GET, path).toString()
        );
        MatcherAssert.assertThat(
            request.root().string(),
            new IsEqual<>(root)
        );
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.StandardRs;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.CompletableFuture;

/**
 * Tests for {@link PathPrefixSlice}.
 */
final class PathPrefixSliceTest {

    @ParameterizedTest
    @CsvSource({
        "'',/,/,",
        "/prefix,/,/prefix/,",
        "/a/b/c,/d/e/f,/a/b/c/d/e/f,",
        "/my/repo,/123/file.txt?param1=foo&param2=bar,/my/repo/123/file.txt,param1=foo&param2=bar",
        "/aaa/bbb,/%26/file.txt?p=%20%20,/aaa/bbb/%26/file.txt,p=%20%20"
    })
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    void shouldAddPrefixToPathAndPreserveEverythingElse(
        final String prefix, final String line, final String path, final String query
    ) {
        final RqMethod method = RqMethod.GET;
        final Headers headers = new Headers.From("X-Header", "The Value");
        final byte[] body = "request body".getBytes();
        new PathPrefixSlice(
            (rsline, rqheaders, rqbody) -> {
                MatcherAssert.assertThat(
                    "Path is modified",
                    new RequestLineFrom(rsline).uri().getRawPath(),
                    new IsEqual<>(path)
                );
                MatcherAssert.assertThat(
                    "Query is preserved",
                    new RequestLineFrom(rsline).uri().getRawQuery(),
                    new IsEqual<>(query)
                );
                MatcherAssert.assertThat(
                    "Method is preserved",
                    new RequestLineFrom(rsline).method(),
                    new IsEqual<>(method)
                );
                MatcherAssert.assertThat(
                    "Headers are preserved",
                    rqheaders,
                    new IsEqual<>(headers)
                );
                MatcherAssert.assertThat(
                    "Body is preserved",
                    new Content.From(rqbody).asBytesFuture().toCompletableFuture().join(),
                    new IsEqual<>(body)
                );
                return StandardRs.OK;
            },
            prefix
        ).response(
            new RequestLine(method, line).toString(),
            headers,
            new Content.From(body)
        ).send(
            (status, rsheaders, rsbody) -> CompletableFuture.allOf()
        );
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.StandardRs;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link UriClientSlice}.
 *
 * @since 0.3
 */
@SuppressWarnings("PMD.UseObjectForClearerAPI")
final class UriClientSliceTest {

    @ParameterizedTest
    @CsvSource({
        "https://artipie.com,true,artipie.com,",
        "http://github.com,false,github.com,",
        "https://github.io:54321,true,github.io,54321",
        "http://localhost:8080,false,localhost,8080"
    })
    void shouldGetClientBySchemeHostPort(
        final String uri, final Boolean secure, final String host, final Integer port
    ) throws Exception {
        final FakeClientSlices fake = new FakeClientSlices((line, headers, body) -> StandardRs.OK);
        new UriClientSlice(
            fake,
            new URI(uri)
        ).response(
            new RequestLine(RqMethod.GET, "/"),
            Headers.EMPTY,
            Content.EMPTY
        ).send(
            (status, rsheaders, rsbody) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Scheme is correct",
            fake.capturedSecure(),
            new IsEqual<>(secure)
        );
        MatcherAssert.assertThat(
            "Host is correct",
            fake.capturedHost(),
            new IsEqual<>(host)
        );
        MatcherAssert.assertThat(
            "Port is correct",
            fake.capturedPort(),
            new IsEqual<>(port)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "http://hostname,/,/,",
        "http://hostname/aaa/bbb,/%26/file.txt?p=%20%20,/aaa/bbb/%26/file.txt,p=%20%20"
    })
    void shouldAddPrefixToPathAndPreserveQuery(
        final String uri, final String line, final String path, final String query
    ) throws Exception {
        new UriClientSlice(
            new FakeClientSlices(
                (rsline, rqheaders, rqbody) -> {
                    MatcherAssert.assertThat(
                        "Path is modified",
                        rsline.uri().getRawPath(),
                        new IsEqual<>(path)
                    );
                    MatcherAssert.assertThat(
                        "Query is preserved",
                        rsline.uri().getRawQuery(),
                        new IsEqual<>(query)
                    );
                    return StandardRs.OK;
                }
            ),
            new URI(uri)
        ).response(
            new RequestLine(RqMethod.GET, line),
            Headers.EMPTY,
            Content.EMPTY
        ).send(
            (status, rsheaders, rsbody) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
    }
}

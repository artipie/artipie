/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * Tests for {@link UriClientSlice}.
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
        String uri, Boolean secure, String host, Integer port
    ) throws Exception {
        final FakeClientSlices fake = new FakeClientSlices(ResponseBuilder.ok().build());
        new UriClientSlice(fake, new URI(uri))
            .response(new RequestLine(RqMethod.GET, "/"), Headers.EMPTY, Content.EMPTY)
            .join();
        Assertions.assertEquals(secure, fake.capturedSecure());
        Assertions.assertEquals(host, fake.capturedHost());
        Assertions.assertEquals(port, fake.capturedPort());
    }

    @ParameterizedTest
    @CsvSource({
        "http://hostname,/,/,",
        "http://hostname/aaa/bbb,/%26/file.txt?p=%20%20,/aaa/bbb/%26/file.txt,p=%20%20"
    })
    void shouldAddPrefixToPathAndPreserveQuery(
        String uri, String line, String path, String query
    ) throws Exception {
        new UriClientSlice(
            new FakeClientSlices(
                (rsline, rqheaders, rqbody) -> {
                    Assertions.assertEquals(path, rsline.uri().getRawPath());
                    Assertions.assertEquals(query, rsline.uri().getRawQuery());
                    return CompletableFuture.completedFuture(ResponseBuilder.ok().build());
                }
            ),
            new URI(uri)
        ).response(new RequestLine(RqMethod.GET, line), Headers.EMPTY, Content.EMPTY)
            .join();
    }
}

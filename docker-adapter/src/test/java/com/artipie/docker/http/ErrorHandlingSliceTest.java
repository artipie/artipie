/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.error.InvalidDigestException;
import com.artipie.docker.error.InvalidManifestException;
import com.artipie.docker.error.InvalidRepoNameException;
import com.artipie.docker.error.InvalidTagNameException;
import com.artipie.docker.error.UnsupportedError;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Response;
import com.artipie.http.client.auth.AuthClientSlice;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseAssert;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.RsStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Stream;

/**
 * Tests for {@link ErrorHandlingSlice}.
 */
class ErrorHandlingSliceTest {

    @Test
    void shouldPassRequestUnmodified() {
        final RequestLine line = new RequestLine(RqMethod.GET, "/file.txt");
        final Header header = new Header("x-name", "some value");
        final byte[] body = "text".getBytes();
        new ErrorHandlingSlice(
            (rqline, rqheaders, rqbody) -> {
                MatcherAssert.assertThat(
                    "Request line unmodified",
                    rqline,
                    new IsEqual<>(line)
                );
                MatcherAssert.assertThat(
                    "Headers unmodified",
                    rqheaders,
                    Matchers.containsInAnyOrder(header)
                );
                MatcherAssert.assertThat(
                    "Body unmodified",
                    rqbody.asBytes(),
                    new IsEqual<>(body)
                );
                return ResponseBuilder.ok().completedFuture();
            }
        ).response(
            line, Headers.from(header), new Content.From(body)
        ).join();
    }

    @Test
    void shouldPassResponseUnmodified() {
        final Header header = new Header("x-name", "some value");
        final byte[] body = "text".getBytes();
        final Response response = new AuthClientSlice(
            (rsline, rsheaders, rsbody) -> ResponseBuilder.ok()
                .header(header).body(body).completedFuture(),
            Authenticator.ANONYMOUS
        ).response(new RequestLine(RqMethod.GET, "/"), Headers.EMPTY, Content.EMPTY)
            .join();
        ResponseAssert.check(response, RsStatus.OK, body, header);
    }

    @ParameterizedTest
    @MethodSource("exceptions")
    void shouldHandleErrorInvalid(RuntimeException exception, RsStatus status, String code) {
        MatcherAssert.assertThat(
            new ErrorHandlingSlice(
                (line, headers, body) -> CompletableFuture.failedFuture(exception)
            ).response(
                new RequestLine(RqMethod.GET, "/"),
                Headers.EMPTY, Content.EMPTY
            ).join(),
            new IsErrorsResponse(status, code)
        );
    }

    @ParameterizedTest
    @MethodSource("exceptions")
    void shouldHandleSliceError(RuntimeException exception, RsStatus status, String code) {
        MatcherAssert.assertThat(
            new ErrorHandlingSlice(
                (line, headers, body) -> {
                    throw exception;
                }
            ).response(new RequestLine(RqMethod.GET, "/"), Headers.EMPTY, Content.EMPTY).
                join(),
            new IsErrorsResponse(status, code)
        );
    }

    @Test
    void shouldPassSliceError() {
        final RuntimeException exception = new IllegalStateException();
        final ErrorHandlingSlice slice = new ErrorHandlingSlice(
            (line, headers, body) -> {
                throw exception;
            }
        );
        final Exception actual = Assertions.assertThrows(
            CompletionException.class,
            () -> slice
                .response(new RequestLine(RqMethod.GET, "/"), Headers.EMPTY, Content.EMPTY)
                .join(),
            "Exception not handled"
        );

        MatcherAssert.assertThat(
            "Original exception preserved",
            actual.getCause(),
            new IsEqual<>(exception)
        );
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static Stream<Arguments> exceptions() {
        final List<Arguments> plain = Stream.concat(
            Stream.of(
                new InvalidRepoNameException("repo name exception"),
                new InvalidTagNameException("tag name exception"),
                new InvalidManifestException("manifest exception"),
                new InvalidDigestException("digest exception")
            ).map(err -> Arguments.of(err, RsStatus.BAD_REQUEST, err.code())),
            Stream.of(
                Arguments.of(
                    new UnsupportedOperationException(),
                    RsStatus.METHOD_NOT_ALLOWED,
                    new UnsupportedError().code()
                )
            )
        ).toList();
        return Stream.concat(
            plain.stream(),
            plain.stream().map(Arguments::get).map(
                original -> Arguments.of(
                    new CompletionException((Throwable) original[0]),
                    original[1],
                    original[2]
                )
            )
        );
    }
}

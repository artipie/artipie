/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */

package com.artipie.http.rq;

import java.net.URI;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link RequestLine}.
 * <p>
 * See 5.1.2 section of
 * <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html">RFC2616</a>
 * </p>
 * @since 0.1
 */
public final class RequestLineFromTest {

    /**
     * Exception message.
     */
    private static final String EX_MSG = "Invalid HTTP request line \n%s";

    @Test
    void parsesMethodName() {
        MatcherAssert.assertThat(
            new RequestLineFrom("TRACE /foo HTTP/1.1\r\n").method(),
            Matchers.equalTo(RqMethod.TRACE)
        );
    }

    @Test
    void parsesAsteriskUri() {
        MatcherAssert.assertThat(
            new RequestLineFrom("GET * HTTP/1.1\r\n").uri(),
            Matchers.equalTo(URI.create("*"))
        );
    }

    @Test
    void parsesAbsoluteUri() {
        MatcherAssert.assertThat(
            new RequestLineFrom("GET http://www.w3.org/pub/WWW/TheProject.html HTTP/1.1\r\n").uri(),
            Matchers.equalTo(URI.create("http://www.w3.org/pub/WWW/TheProject.html"))
        );
    }

    @Test
    void parsesAbsolutePath() {
        MatcherAssert.assertThat(
            new RequestLineFrom("GET /pub/WWW/TheProject.html HTTP/1.1\r\n").uri(),
            Matchers.equalTo(URI.create("/pub/WWW/TheProject.html"))
        );
    }

    @Test
    void parsesHttpVersion() {
        MatcherAssert.assertThat(
            new RequestLineFrom("PUT * HTTP/1.1\r\n").version(),
            Matchers.equalTo("HTTP/1.1")
        );
    }

    @Test
    void throwsExceptionIfMethodIsUnknown() {
        final String method = "SURRENDER";
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                IllegalStateException.class,
                () -> new RequestLineFrom(
                    String.format("%s /wallet/or/life HTTP/1.1\n", method)
                ).method()
            ).getMessage(),
            new IsEqual<>(String.format("Unknown method: '%s'", method))
        );
    }

    @Test
    void throwsExceptionIfLineIsShortInvalid() {
        final String line = "fake";
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new RequestLineFrom(line).version()
            ).getMessage(),
            new IsEqual<>(String.format(RequestLineFromTest.EX_MSG, line))
        );
    }

    @Test
    void throwsExceptionIfLineIsLongInvalid() {
        final String line = "GET /beer/in/the/pub.html /wine/in/the/restaurant.html HTTP/1.1\n";
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new RequestLineFrom(line).version()
            ).getMessage(),
            new IsEqual<>(String.format(RequestLineFromTest.EX_MSG, line))
        );
    }
}

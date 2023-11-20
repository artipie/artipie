/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rq;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Ensure that {@link RequestLine} works correctly.
 *
 * @since 0.1
 */
public class RequestLineTest {

    @Test
    public void reqLineStringIsCorrect() {
        MatcherAssert.assertThat(
            new RequestLine("GET", "/pub/WWW/TheProject.html", "HTTP/1.1").toString(),
            Matchers.equalTo("GET /pub/WWW/TheProject.html HTTP/1.1\r\n")
        );
    }

    @Test
    public void shouldHaveDefaultVersionWhenNoneSpecified() {
        MatcherAssert.assertThat(
            new RequestLine(RqMethod.PUT, "/file.txt").toString(),
            Matchers.equalTo("PUT /file.txt HTTP/1.1\r\n")
        );
    }
}

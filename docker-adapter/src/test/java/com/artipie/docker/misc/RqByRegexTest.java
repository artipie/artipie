/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.misc;

import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

/**
 * Test for {@link RqByRegex}.
 */
class RqByRegexTest {

    @Test
    void shouldMatchPath() {
        Assertions.assertTrue(
            new RqByRegex(new RequestLine(RqMethod.GET, "/v2/some/repo"),
                Pattern.compile("/v2/.*")).path().matches()
        );
    }

    @Test
    void shouldThrowExceptionIsDoesNotMatch() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new RqByRegex(new RequestLine(RqMethod.GET, "/v3/my-repo/blobs"),
                Pattern.compile("/v2/.*/blobs")).path()
        );
    }

}

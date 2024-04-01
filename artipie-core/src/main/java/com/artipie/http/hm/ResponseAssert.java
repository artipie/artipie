/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.hm;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.headers.Header;
import com.artipie.http.RsStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResponseAssert {

    public static void checkOk(Response actual) {
        check(actual, RsStatus.OK);
    }

    public static void check(Response actual, RsStatus status) {
        MatcherAssert.assertThat(actual.status(), Matchers.is(status));
    }

    public static void check(Response actual, RsStatus status, Header... headers) {
        MatcherAssert.assertThat(actual.status(), Matchers.is(status));
        checkHeaders(actual.headers(), headers);
    }

    public static void check(Response actual, RsStatus status, byte[] body, Header... headers) {
        MatcherAssert.assertThat(actual.status(), Matchers.is(status));
        checkHeaders(actual.headers(), headers);
        MatcherAssert.assertThat(actual.body().asBytes(), Matchers.is(body));
    }

    public static void check(Response actual, RsStatus status, byte[] body) {
        MatcherAssert.assertThat(actual.status(), Matchers.is(status));
        MatcherAssert.assertThat(actual.body().asBytes(), Matchers.is(body));
    }

    public static void check(Response actual, byte[] body) {
        MatcherAssert.assertThat(actual.body().asBytes(), Matchers.is(body));
    }

    private static void checkHeaders(Headers actual, Header... expected) {
        Arrays.stream(expected).forEach(h -> checkHeader(actual, h));
    }

    private static void checkHeader(Headers actual, Header header) {
        List<Header> list = actual.find(header.getKey());
        MatcherAssert.assertThat("Actual headers doesn't contain '" + header.getKey() + "'",
            list.isEmpty(), Matchers.is(false));
        Optional<Header> res = list.stream()
            .filter(h -> Objects.equals(h, header))
            .findAny();
        if (res.isEmpty()) {
            throw new AssertionError(
                "'" + header.getKey() + "' header values don't match: expected=" + header.getValue()
                    + ", actual=" + list.stream().map(Header::getValue).collect(Collectors.joining(", "))
            );
        }
    }
}

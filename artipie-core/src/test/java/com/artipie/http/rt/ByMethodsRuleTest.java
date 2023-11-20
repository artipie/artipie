/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rt;

import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link ByMethodsRule}.
 *
 * @since 0.16
 */
final class ByMethodsRuleTest {

    @Test
    void matchesExpectedMethod() {
        MatcherAssert.assertThat(
            new ByMethodsRule(RqMethod.GET, RqMethod.POST).apply(
                new RequestLine(RqMethod.GET, "/").toString(),
                Collections.emptyList()
            ),
            Matchers.is(true)
        );
    }

    @Test
    void doesntMatchUnexpectedMethod() {
        MatcherAssert.assertThat(
            new ByMethodsRule(RqMethod.GET, RqMethod.POST).apply(
                new RequestLine(RqMethod.DELETE, "/").toString(),
                Collections.emptyList()
            ),
            Matchers.is(false)
        );
    }
}

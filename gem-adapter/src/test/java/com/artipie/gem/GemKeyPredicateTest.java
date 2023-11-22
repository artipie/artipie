/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem;

import com.artipie.asto.Key;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test case for {@link GemKeyPredicate}.
 * @since 1.3
 */
final class GemKeyPredicateTest {

    @ParameterizedTest
    @CsvSource({
        "builder,builder-3.2.4.gem",
        "file-tail,file-tail-1.2.0.gem",
        "gviz,gviz-0.3.5.gem",
        "rails,rails-6.0.2.2.gem",
        "builder,gems/builder-3.2.4.gem"
    })
    void testCorrectItems(final String name, final String target) {
        MatcherAssert.assertThat(
            String.format("`%s` not matched by `%s`", target, name),
            new GemKeyPredicate(name).test(new Key.From(target)),
            Matchers.is(true)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "builder,builder-3.2.4",
        "file-tail,file-tail.gem",
        "gviz,builder-0.3.5.gem",
        "rails,6.0.2.2.gem"
    })
    void testWrongItems(final String name, final String target) {
        MatcherAssert.assertThat(
            String.format("`%s` matched by `%s`", target, name),
            new GemKeyPredicate(name).test(new Key.From(target)),
            Matchers.is(false)
        );
    }
}

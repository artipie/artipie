/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.security.perms;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link Action.Standard}.
 * @since 1.2
 * @checkstyle MagicNumberCheck (500 lines)
 */
public final class StandardActionTest {

    @ParameterizedTest
    @ValueSource(strings = {"r", "read", "install", "download", "pull"})
    void readsAllReadActionSynonyms(final String name) {
        MatcherAssert.assertThat(
            Action.Standard.maskByAction(name),
            new IsEqual<>(0x4)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"w", "write", "publish", "push", "deploy", "upload"})
    void readsAllWriteActionSynonyms(final String name) {
        MatcherAssert.assertThat(
            Action.Standard.maskByAction(name),
            new IsEqual<>(0x2)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"d", "delete", "remove"})
    void readsAllDeleteActionSynonyms(final String name) {
        MatcherAssert.assertThat(
            Action.Standard.maskByAction(name),
            new IsEqual<>(0x8)
        );
    }
}

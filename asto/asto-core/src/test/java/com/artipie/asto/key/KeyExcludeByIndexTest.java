/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.key;

import com.artipie.asto.Key;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link KeyExcludeByIndex}.
 *
 * @since 1.9.1
 */
final class KeyExcludeByIndexTest {

    @Test
    void excludesPart() {
        final Key key = new Key.From("1", "2", "1");
        MatcherAssert.assertThat(
            new KeyExcludeByIndex(key, 0).string(),
            new IsEqual<>("2/1")
        );
    }

    @Test
    void excludesNonExistingPart() {
        final Key key = new Key.From("1", "2");
        MatcherAssert.assertThat(
            new KeyExcludeByIndex(key, -1).string(),
            new IsEqual<>("1/2")
        );
    }
}

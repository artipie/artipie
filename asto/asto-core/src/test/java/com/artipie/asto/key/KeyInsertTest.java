/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.key;

import com.artipie.asto.Key;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link KeyInsert}.
 *
 * @since 1.9.1
 */
final class KeyInsertTest {

    @Test
    void insertsPart() {
        final Key key = new Key.From("1", "2", "4");
        MatcherAssert.assertThat(
            new KeyInsert(key, "3", 2).string(),
            new IsEqual<>("1/2/3/4")
        );
    }

    @Test
    void insertsIndexOutOfBounds() {
        final Key key = new Key.From("1", "2");
        Assertions.assertThrows(
            IndexOutOfBoundsException.class,
            () -> new KeyInsert(key, "3", -1)
        );
    }
}

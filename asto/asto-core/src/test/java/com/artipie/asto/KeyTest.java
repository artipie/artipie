/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link Key}.
 *
 * @since 0.32
 */
@SuppressWarnings("PMD.TooManyMethods")
final class KeyTest {

    @Test
    void getPartsOfKey() {
        final Key key = new Key.From("1", "2");
        MatcherAssert.assertThat(
            key.parts(),
            Matchers.containsInRelativeOrder("1", "2")
        );
    }

    @Test
    void resolvesKeysFromParts() {
        MatcherAssert.assertThat(
            new Key.From("one1", "two2", "three3/four4").string(),
            new IsEqual<>("one1/two2/three3/four4")
        );
    }

    @Test
    void resolvesKeyFromParts() {
        MatcherAssert.assertThat(
            new Key.From("one", "two", "three").string(),
            Matchers.equalTo("one/two/three")
        );
    }

    @Test
    void resolvesKeyFromBasePath() {
        MatcherAssert.assertThat(
            new Key.From(new Key.From("black", "red"), "green", "yellow").string(),
            Matchers.equalTo("black/red/green/yellow")
        );
    }

    @Test
    void keyFromString() {
        final String string = "a/b/c";
        MatcherAssert.assertThat(
            new Key.From(string).string(),
            Matchers.equalTo(string)
        );
    }

    @Test
    void keyWithEmptyPart() {
        Assertions.assertThrows(Exception.class, () -> new Key.From("", "something").string());
    }

    @Test
    void resolvesRootKey() {
        MatcherAssert.assertThat(Key.ROOT.string(), Matchers.equalTo(""));
    }

    @Test
    void returnsParent() {
        MatcherAssert.assertThat(
            new Key.From("a/b").parent().get().string(),
            new IsEqual<>("a")
        );
    }

    @Test
    void rootParent() {
        MatcherAssert.assertThat(
            "ROOT parent is not empty",
            !Key.ROOT.parent().isPresent()
        );
    }

    @Test
    void emptyKeyParent() {
        MatcherAssert.assertThat(
            "Empty key parent is not empty",
            !new Key.From("").parent().isPresent()
        );
    }

    @Test
    void comparesKeys() {
        final Key frst = new Key.From("1");
        final Key scnd = new Key.From("2");
        MatcherAssert.assertThat(
            Key.CMP_STRING.compare(frst, scnd),
            new IsEqual<>(-1)
        );
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker;

import com.artipie.docker.error.InvalidTagNameException;
import com.artipie.docker.misc.Validator;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

/**
 * Tests for {@link Tag.Valid}.
 */
class TagValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "latest",
        "1.0",
        "my-tag",
        "MY_TAG",
        "My.Tag.1",
        "_some_tag",
        "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567"
    })
    void shouldGetValueWhenValid(final String tag) {
        Assertions.assertTrue(Validator.isValidTag(tag));
        Assertions.assertEquals(tag, Validator.validateTag(tag));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "",
        ".0",
        "*",
        "\u00ea",
        "-my-tag",
        "012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678"
    })
    void shouldFailToGetValueWhenInvalid(final String tag) {
        Assertions.assertFalse(Validator.isValidTag(tag));
        final Throwable throwable = Assertions.assertThrows(
            InvalidTagNameException.class, () -> Validator.validateTag(tag)
        );
        MatcherAssert.assertThat(
            throwable.getMessage(),
            new AllOf<>(
                Arrays.asList(
                    new StringContains(true, "Invalid tag"),
                    new StringContains(false, tag)
                )
            )
        );
    }
}

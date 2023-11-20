/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.meta;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ChangelogEntry}.
 *
 * @since 0.8.3
 */
class ChangelogEntryTest {

    @Test
    void shouldParseAuthor() {
        MatcherAssert.assertThat(
            new ChangelogEntry(
                "* Wed May 12 2020 John Doe <johndoe@artipie.org> - 0.1-2\n- Second artipie package"
            ).author(),
            new IsEqual<>("John Doe <johndoe@artipie.org>")
        );
    }

    @Test
    @SuppressWarnings("PMD.UseUnderscoresInNumericLiterals")
    void shouldParseDate() {
        final int unixtime = 1589328000;
        MatcherAssert.assertThat(
            new ChangelogEntry(
                "* Wed May 13 2020 John Doe <johndoe@artipie.org> - 0.1-2\n- Second artipie package"
            ).date(),
            new IsEqual<>(unixtime)
        );
    }

    @Test
    void shouldFailParseBadDate() {
        final ChangelogEntry entry = new ChangelogEntry(
            "* Abc March 41 20 John Doe <johndoe@artipie.org> - 0.1-2\n- Second artipie package"
        );
        Assertions.assertThrows(IllegalStateException.class, entry::date);
    }

    @Test
    void shouldParseContent() {
        MatcherAssert.assertThat(
            new ChangelogEntry(
                "* Wed May 14 2020 John Doe <johndoe@artipie.org> - 0.1-2\n- Second artipie package"
            ).content(),
            new IsEqual<>("- 0.1-2\n- Second artipie package")
        );
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.gem;

import org.cactoos.iterable.IterableOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link YamlMetaFormat}.
 * @since 1.3
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class YamlMetaFormatTest {
    @Test
    void addPlainString() {
        final String key = "os";
        final String val = "macos";
        final YamlMetaFormat.Yamler yaml = new YamlMetaFormat.Yamler();
        new YamlMetaFormat(yaml).print(key, val);
        MatcherAssert.assertThat(
            yaml.build().toString(), new IsEqual<>("os: macos")
        );
    }

    @Test
    void addArray() {
        final String key = "deps";
        final String[] items = new String[] {"java", "ruby", "go"};
        final YamlMetaFormat.Yamler yaml = new YamlMetaFormat.Yamler();
        new YamlMetaFormat(yaml).print(key, items);
        MatcherAssert.assertThat(
            yaml.build().toString(),
            new StringContainsInOrder(
                new IterableOf<>("deps:", "java", "ruby", "go")
            )
        );
    }

    @Test
    void addNestedObjects() {
        final String root = "root";
        final String child = "child";
        final String key = "key";
        final String val = "value";
        final YamlMetaFormat.Yamler yaml = new YamlMetaFormat.Yamler();
        new YamlMetaFormat(yaml).print(
            root, first -> first.print(child, second -> second.print(key, val))
        );
        MatcherAssert.assertThat(
            yaml.build().toString(),
            new StringContainsInOrder(
                new IterableOf<>("root:", "child:", "key: value")
            )
        );
    }
}

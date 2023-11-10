/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.gem;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * Test case for {@link JsonMetaFormat}.
 * @since 1.3
 */
final class JsonMetaFormatTest {
    @Test
    void addPlainString() {
        final String key = "str";
        final String val = "qwerty";
        final JsonObjectBuilder json = Json.createObjectBuilder();
        new JsonMetaFormat(json).print(key, val);
        MatcherAssert.assertThat(
            json.build(), new JsonHas(key, val)
        );
    }

    @Test
    void addArray() {
        final String key = "numbers";
        final Collection<String> items = Arrays.asList("one", "two", "three");
        final JsonObjectBuilder json = Json.createObjectBuilder();
        new JsonMetaFormat(json).print(key, items.toArray(new String[0]));
        MatcherAssert.assertThat(
            json.build(),
            new JsonHas(
                key, new JsonContains(
                    items.stream().map(JsonValueIs::new)
                        .collect(Collectors.toList())
                )
            )
        );
    }

    @Test
    void addNestedObjects() {
        final String root = "root";
        final String child = "child";
        final String key = "key";
        final String val = "value";
        final JsonObjectBuilder json = Json.createObjectBuilder();
        new JsonMetaFormat(json).print(
            root, first -> first.print(child, second -> second.print(key, val))
        );
        MatcherAssert.assertThat(
            json.build(),
            new JsonHas(root, new JsonHas(child, new JsonHas(key, val)))
        );
    }
}

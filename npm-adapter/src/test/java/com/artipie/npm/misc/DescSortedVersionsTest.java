/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.misc;

import javax.json.Json;
import javax.json.JsonObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test cases for {@link DescSortedVersions}.
 * @since 0.9
 */
final class DescSortedVersionsTest {

    @Test
    void sortsVersionsInDescendingOrder() {
        final JsonObject versions =
            Json.createObjectBuilder()
                .add("1", "")
                .add("2", "1.1")
                .add("3", "1.1.1")
                .add("4", "1.2.1")
                .add("5", "1.3.0")
                .build();
        MatcherAssert.assertThat(
            new DescSortedVersions(versions).value(),
            Matchers.contains("5", "4", "3", "2", "1")
        );
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Key;
import java.util.Arrays;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonValueIs;
import wtf.g4s8.hamcrest.json.StringIsJson;

/**
 * Test case for {@link ListingFormat}.
 * @since 1.1.1
 */
public class ListingFormatTest {
    @Test
    void formatTextKeys() {
        MatcherAssert.assertThat(
            ListingFormat.Standard.TEXT.apply(
                Arrays.asList(
                    new Key.From("a", "file.txt"),
                    new Key.From("c.txt"),
                    new Key.From("b", "file2.txt")
                )
            ),
            Matchers.equalTo("a/file.txt\nc.txt\nb/file2.txt")
        );
    }

    @Test
    void formatTextEmptyKeys() {
        MatcherAssert.assertThat(
            ListingFormat.Standard.TEXT.apply(Collections.emptyList()),
            Matchers.emptyString()
        );
    }

    @Test
    void formatJsonKeys() {
        MatcherAssert.assertThat(
            ListingFormat.Standard.JSON.apply(
                Arrays.asList(
                    new Key.From("one", "file.bin"),
                    new Key.From("two", "file3.bin"),
                    new Key.From("three", "file2.bin")
                )
            ),
            new StringIsJson.Array(
                new JsonContains(
                    new JsonValueIs("one/file.bin"),
                    new JsonValueIs("two/file3.bin"),
                    new JsonValueIs("three/file2.bin")
                )
            )
        );
    }

    @Test
    void formatJsonEmptyKeys() {
        MatcherAssert.assertThat(
            ListingFormat.Standard.JSON.apply(Collections.emptyList()),
            new StringIsJson.Array(new JsonContains())
        );
    }

    @Test
    void formatHtmlKeys() {
        MatcherAssert.assertThat(
            ListingFormat.Standard.HTML.apply(
                Arrays.asList(
                    new Key.From("example0.log"),
                    new Key.From("one/", "example1.log"),
                    new Key.From("one/two/", "example2.log")
                )
            ),
            new StringContainsInOrder(
                Arrays.asList(
                    "<head>",
                    "</head",
                    "<body>",
                    "  <ul>",
                    "    <li><a href=",
                    "  </ul>",
                    "</body>"
                ))
        );
    }

    @Test
    void formatHtmlEmptyKeys() {
        MatcherAssert.assertThat(
            ListingFormat.Standard.HTML.apply(Collections.emptyList()),
            Matchers.is(
                String.join(
                    "\n",
                    "<!DOCTYPE html>",
                    "<html>",
                    "  <head><meta charset=\"utf-8\"/></head>",
                    "  <body>",
                    "    <ul>",
                    "",
                    "    </ul>",
                    "  </body>",
                    "</html>"
                )
            )
        );
    }
}

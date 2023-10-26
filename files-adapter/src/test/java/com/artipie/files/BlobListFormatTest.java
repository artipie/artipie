/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.files;

import com.artipie.asto.Key;
import java.util.Arrays;
import java.util.Collections;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonContains;
import wtf.g4s8.hamcrest.json.JsonValueIs;
import wtf.g4s8.hamcrest.json.StringIsJson;

/**
 * Test case for {@link BlobListFormat}.
 * @since 1.0
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class BlobListFormatTest {

    @Test
    void formatTextKeys() {
        MatcherAssert.assertThat(
            BlobListFormat.Standard.TEXT.apply(
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
            BlobListFormat.Standard.TEXT.apply(Collections.emptyList()),
            Matchers.emptyString()
        );
    }

    @Test
    void formatJsonKeys() {
        MatcherAssert.assertThat(
            BlobListFormat.Standard.JSON.apply(
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
            BlobListFormat.Standard.JSON.apply(Collections.emptyList()),
            new StringIsJson.Array(new JsonContains())
        );
    }

    @Test
    void formatHtmlKeys() {
        MatcherAssert.assertThat(
            BlobListFormat.Standard.HTML.apply(
                Arrays.asList(
                    new Key.From("foo/barx", "file1.log"),
                    new Key.From("foo/bary", "file2.log"),
                    new Key.From("foo/barz", "file3.log")
                )
            ),
            Matchers.is(
                String.join(
                    "\n",
                    "<!DOCTYPE html>",
                    "<html>",
                    "  <head><meta charset=\"utf-8\"/></head>",
                    "  <body>",
                    "    <ul>",
                    "      <li><a href=\"/foo/barx/file1.log\">foo/barx/file1.log</a></li>",
                    "      <li><a href=\"/foo/bary/file2.log\">foo/bary/file2.log</a></li>",
                    "      <li><a href=\"/foo/barz/file3.log\">foo/barz/file3.log</a></li>",
                    "    </ul>",
                    "  </body>",
                    "</html>"
                )
            )
        );
    }

    @Test
    void formatHtmlEmptyKeys() {
        MatcherAssert.assertThat(
            BlobListFormat.Standard.HTML.apply(Collections.emptyList()),
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

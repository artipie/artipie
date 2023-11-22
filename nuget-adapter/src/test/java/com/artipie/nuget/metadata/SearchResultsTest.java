/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.metadata;

import com.artipie.asto.test.TestResource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.cactoos.list.ListOf;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link SearchResults}.
 * @since 1.2
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class SearchResultsTest {

    @Test
    void writesJsonResult() throws IOException, JSONException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new SearchResults(out).generate(
            new ListOf<SearchResults.Package>(
                new SearchResults.Package(
                    "AbcPackage", new ListOf<>("Dependency"),
                    new ListOf<>(new SearchResults.Version("0.0.1", 123, "http://link"))
                ),
                new SearchResults.Package(
                    "XyzPackage", new ListOf<>("Library", "Dependency"),
                    new ListOf<>(
                        new SearchResults.Version("0.1", 345, "http://link/0.1"),
                        new SearchResults.Version("0.1.2", 23, "http://link/0.1.2"),
                        new SearchResults.Version("0.0.1", 1, "http://link/0.0.1")
                    )
                )
            )
        );
        JSONAssert.assertEquals(
            out.toString(StandardCharsets.UTF_8.name()),
            new String(
                new TestResource("SearchResultsTest/writesJsonResult.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            true
        );
    }

}

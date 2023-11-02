/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.headers.ContentDisposition;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link DownloadRepodataSlice}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class DownloadRepodataSliceTest {

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void returnsItemFromStorageIfExists() {
        final byte[] bytes = "data".getBytes();
        this.asto.save(
            new Key.From("linux-64/repodata.json"), new Content.From(bytes)
        ).join();
        MatcherAssert.assertThat(
            new DownloadRepodataSlice(this.asto),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasBody(bytes),
                    new RsHasHeaders(
                        new ContentDisposition("attachment; filename=\"repodata.json\""),
                        new ContentLength(bytes.length)
                    )
                ),
                new RequestLine(RqMethod.GET, "any/other/parts/linux-64/repodata.json")
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"current_repodata.json", "repodata.json"})
    void returnsEmptyJsonIfNotExists(final String filename) {
        final byte[] bytes = "{\"info\":{\"subdir\":\"noarch\"}}".getBytes();
        MatcherAssert.assertThat(
            new DownloadRepodataSlice(this.asto),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasBody(bytes),
                    new RsHasHeaders(
                        new ContentDisposition(
                            String.format("attachment; filename=\"%s\"", filename)
                        ),
                        new ContentLength(bytes.length)
                    )
                ),
                new RequestLine(RqMethod.GET, String.format("/noarch/%s", filename))
            )
        );
    }
}

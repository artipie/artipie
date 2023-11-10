/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.hex.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.hex.ResourceUtil;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.ContentType;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.nio.file.Files;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link DownloadSlice}.
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DownloadSliceTest {

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Download slice.
     */
    private Slice slice;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.slice = new DownloadSlice(this.storage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/packages/not_artifact", "/tarballs/not_artifact-0.1.0.tar"})
    void notFound(final String path) {
        MatcherAssert.assertThat(
            this.slice,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, path)
            )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"packages/decimal", "tarballs/decimal-2.0.0.tar"})
    void downloadOk(final String path) throws Exception {
        final byte[] bytes = Files.readAllBytes(new ResourceUtil(path).asPath());
        this.storage.save(new Key.From(path), new Content.From(bytes));
        MatcherAssert.assertThat(
            this.slice,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.GET, String.format("/%s", path)),
                new Headers.From(
                    new Headers.From(new ContentType("application/octet-stream")),
                    new Headers.From(new ContentLength(bytes.length))
                ),
                new Content.From(bytes)
            )
        );
    }
}

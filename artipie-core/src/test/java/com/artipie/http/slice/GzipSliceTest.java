/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.http.Headers;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.RsStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

/**
 * Test for {@link GzipSlice}.
 */
class GzipSliceTest {

    @Test
    void returnsGzipedContentPreservesStatusAndHeaders() throws IOException {
        final byte[] data = "any byte data".getBytes(StandardCharsets.UTF_8);
        final Header hdr = new Header("any-header", "value");
        MatcherAssert.assertThat(
            new GzipSlice(
                new SliceSimple(
                    ResponseBuilder.from(RsStatus.MOVED_TEMPORARILY)
                        .header(hdr).body(data).build()
                )
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.MOVED_TEMPORARILY),
                    new RsHasHeaders(
                        Headers.from(
                            new Header("Content-encoding", "gzip"),
                            hdr,
                            new ContentLength(13)
                        )
                    ),
                    new RsHasBody(GzipSliceTest.gzip(data))
                ),
                new RequestLine(RqMethod.GET, "/any")
            )
        );
    }

    static byte[] gzip(final byte[] data) throws IOException {
        final ByteArrayOutputStream res = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(res)) {
            gzos.write(data);
            gzos.finish();
        }
        return res.toByteArray();
    }

}

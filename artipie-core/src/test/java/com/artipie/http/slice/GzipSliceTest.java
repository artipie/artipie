/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.RsStatus;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseAssert;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
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
        ResponseAssert.check(
            new GzipSlice(
                new SliceSimple(
                    ResponseBuilder.from(RsStatus.MOVED_TEMPORARILY)
                        .header(hdr).body(data).build()
                )
            ).response(
                new RequestLine(RqMethod.GET, "/any"), Headers.EMPTY, Content.EMPTY
            ).join(),
            RsStatus.MOVED_TEMPORARILY,
            GzipSliceTest.gzip(data),
            new Header("Content-encoding", "gzip"),
            new ContentLength(13),
            hdr
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

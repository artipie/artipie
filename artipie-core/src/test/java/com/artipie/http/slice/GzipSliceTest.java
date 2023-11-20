/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link GzipSlice}.
 * @since 1.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class GzipSliceTest {

    @Test
    void returnsGzipedContentPreservesStatusAndHeaders() throws IOException {
        final byte[] data = "any byte data".getBytes(StandardCharsets.UTF_8);
        final Header hdr = new Header("any-header", "value");
        MatcherAssert.assertThat(
            new GzipSlice(
                new SliceSimple(
                    new RsFull(RsStatus.FOUND, new Headers.From(hdr), new Content.From(data))
                )
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.FOUND),
                    new RsHasHeaders(
                        new Headers.From(
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

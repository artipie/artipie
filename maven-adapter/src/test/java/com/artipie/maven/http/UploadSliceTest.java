/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.ContentIs;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link UploadSlice}.
 * @since 0.8
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class UploadSliceTest {

    /**
     * Test storage.
     */
    private Storage asto;

    /**
     * Update maven slice.
     */
    private Slice ums;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
        this.ums = new UploadSlice(this.asto);
    }

    @Test
    void savesDataToTempUpload() {
        final byte[] data = "jar content".getBytes();
        MatcherAssert.assertThat(
            "Wrong response status, CREATED is expected",
            this.ums,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(RqMethod.PUT, "/com/artipie/asto/0.1/asto-0.1.jar"),
                new Headers.From(new ContentLength(data.length)),
                new Content.From(data)
            )
        );
        MatcherAssert.assertThat(
            "Uploaded data were not saved to storage",
            this.asto.value(new Key.From(".upload/com/artipie/asto/0.1/asto-0.1.jar")).join(),
            new ContentIs(data)
        );
    }

}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.RsStatus;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DeleteSliceTest {

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void testDelete() {
        final byte[] content = "python package".getBytes();
        final String key = "simple/test-pack-1.0.0.tar.gz";
        this.asto.save(new Key.From(key), new Content.From(content)).join();

        MatcherAssert.assertThat(
                "Response is OK",
                new DeleteSlice(this.asto),
                new SliceHasResponse(
                        new RsHasStatus(RsStatus.OK),
                        new RequestLine(RqMethod.DELETE, "simple/test-pack-1.0.0.tar.gz")
                )
        );

        MatcherAssert.assertThat(
                "Response is OK",
                new DeleteSlice(this.asto),
                new SliceHasResponse(
                        new RsHasStatus(RsStatus.NOT_FOUND),
                        new RequestLine(RqMethod.DELETE, "simple/test-pack-1.0.1.tar.gz")
                )
        );
    }
}

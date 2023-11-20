/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.headers.ContentDisposition;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HeadSlice}.
 *
 * @since 0.26.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class HeadSliceTest {

    /**
     * Storage.
     */
    private final Storage storage = new InMemoryStorage();

    @Test
    void returnsFound() {
        final Key key = new Key.From("foo");
        final Key another = new Key.From("bar");
        new BlockingStorage(this.storage).save(key, "anything".getBytes());
        new BlockingStorage(this.storage).save(another, "another".getBytes());
        MatcherAssert.assertThat(
            new HeadSlice(this.storage),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasHeaders(
                        // @checkstyle MagicNumberCheck (1 line)
                        new ContentLength(8),
                        new ContentDisposition("attachment; filename=\"foo\"")
                    ),
                    new RsHasBody(StringUtils.EMPTY)
                ),
                new RequestLine(RqMethod.HEAD, "/foo")
            )
        );
    }

    @Test
    void returnsNotFound() {
        MatcherAssert.assertThat(
            new SliceDelete(this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.DELETE, "/bar")
            )
        );
    }
}


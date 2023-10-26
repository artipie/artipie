/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.composer.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.composer.AstoRepository;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DownloadArchiveSlice}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class DownloadArchiveSliceTest {
    @Test
    void returnsOkStatus() {
        final Storage storage = new InMemoryStorage();
        final String archive = "log-1.1.3.zip";
        final Key key = new Key.From("artifacts", archive);
        new TestResource(archive)
            .saveTo(storage, key);
        MatcherAssert.assertThat(
            new DownloadArchiveSlice(new AstoRepository(storage)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.GET, key.string()),
                Headers.EMPTY,
                new Content.From(new TestResource(archive).asBytes())
            )
        );
    }
}

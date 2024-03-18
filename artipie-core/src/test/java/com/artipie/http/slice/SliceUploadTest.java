/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.scheduling.RepositoryEvents;
import io.reactivex.Flowable;
import org.cactoos.map.MapEntry;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Test case for {@link SliceUpload}.
 */
public final class SliceUploadTest {

    @Test
    void uploadsKeyByPath() throws Exception {
        final Storage storage = new InMemoryStorage();
        final String hello = "Hello";
        final byte[] data = hello.getBytes(StandardCharsets.UTF_8);
        final String path = "uploads/file.txt";
        MatcherAssert.assertThat(
            "Wrong HTTP status returned",
            new SliceUpload(storage).response(
                new RequestLine("PUT", path, "HTTP/1.1"),
                Headers.from(
                    new MapEntry<>("Content-Size", Long.toString(data.length))
                ),
                Flowable.just(ByteBuffer.wrap(data))
            ),
            new RsHasStatus(RsStatus.CREATED)
        );
        MatcherAssert.assertThat(
            new String(
                new Remaining(
                    Flowable.fromPublisher(storage.value(new Key.From(path)).get()).toList()
                        .blockingGet().get(0)
                ).bytes(),
                StandardCharsets.UTF_8
            ),
            new IsEqual<>(hello)
        );
    }

    @Test
    void logsEventOnUpload() {
        final byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);
        final Queue<ArtifactEvent> queue = new LinkedList<>();
        MatcherAssert.assertThat(
            "Wrong HTTP status returned",
            new SliceUpload(new InMemoryStorage(), new RepositoryEvents("files", "my-repo", queue))
                .response(
                    new RequestLine("PUT", "uploads/file.txt", "HTTP/1.1"),
                    Headers.from("Content-Size", Long.toString(data.length)),
                    Flowable.just(ByteBuffer.wrap(data))
                ),
            new RsHasStatus(RsStatus.CREATED)
        );
        MatcherAssert.assertThat("Event was added to queue", queue.size() == 1);
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.slice;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.scheduling.RepositoryEvents;
import java.util.LinkedList;
import java.util.Queue;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SliceDelete}.
 *
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class SliceDeleteTest {

    /**
     * Storage.
     */
    private final Storage storage = new InMemoryStorage();

    @Test
    void deleteCorrectEntry() throws Exception {
        final Key key = new Key.From("foo");
        final Key another = new Key.From("bar");
        new BlockingStorage(this.storage).save(key, "anything".getBytes());
        new BlockingStorage(this.storage).save(another, "another".getBytes());
        MatcherAssert.assertThat(
            "Didn't respond with NO_CONTENT status",
            new SliceDelete(this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NO_CONTENT),
                new RequestLine(RqMethod.DELETE, "/foo")
            )
        );
        MatcherAssert.assertThat(
            "Didn't delete from storage",
            new BlockingStorage(this.storage).exists(key),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Deleted another key",
            new BlockingStorage(this.storage).exists(another),
            new IsEqual<>(true)
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

    @Test
    void logsEventOnDelete() {
        final Key key = new Key.From("foo");
        final Key another = new Key.From("bar");
        new BlockingStorage(this.storage).save(key, "anything".getBytes());
        new BlockingStorage(this.storage).save(another, "another".getBytes());
        final Queue<ArtifactEvent> queue = new LinkedList<>();
        MatcherAssert.assertThat(
            "Didn't respond with NO_CONTENT status",
            new SliceDelete(this.storage, new RepositoryEvents("files", "my-repo", queue)),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NO_CONTENT),
                new RequestLine(RqMethod.DELETE, "/foo")
            )
        );
        MatcherAssert.assertThat("Event was added to queue", queue.size() == 1);
    }
}


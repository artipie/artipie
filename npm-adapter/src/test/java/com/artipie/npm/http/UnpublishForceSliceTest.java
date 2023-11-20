/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.scheduling.ArtifactEvent;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link UnpublishForceSlice}.
 * @since 0.8
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class UnpublishForceSliceTest {
    /**
     * Storage.
     */
    private Storage storage;

    /**
     * Test artifact events.
     */
    private Queue<ArtifactEvent> events;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.events = new LinkedList<>();
    }

    @Test
    void returnsOkAndDeletePackage() {
        new TestResource("storage").addFilesTo(this.storage, Key.ROOT);
        MatcherAssert.assertThat(
            "Response status is OK",
            new UnpublishForceSlice(
                this.storage, Optional.of(this.events), UnpublishPutSliceTest.REPO
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(
                    RqMethod.DELETE, "/@hello%2fsimple-npm-project/-rev/undefined"
                ),
                Headers.EMPTY,
                Content.EMPTY
            )
        );
        MatcherAssert.assertThat(
            "The entire package was removed",
            this.storage.list(new Key.From("@hello/simple-npm-project"))
                .join().isEmpty(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat("Events queue has one item", this.events.size() == 1);
    }

    @Test
    void returnsBadRequest() {
        MatcherAssert.assertThat(
            new UnpublishForceSlice(
                this.storage, Optional.of(this.events), UnpublishPutSliceTest.REPO
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.GET, "/bad/request")
            )
        );
        MatcherAssert.assertThat("Events queue is empty", this.events.size() == 0);
    }
}

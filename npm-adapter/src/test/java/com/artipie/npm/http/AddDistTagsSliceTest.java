/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AddDistTagsSlice}.
 * @since 0.8
 */
class AddDistTagsSliceTest {

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Meta file key.
     */
    private Key meta;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.meta = new Key.From("@hello/simple-npm-project", "meta.json");
        this.storage.save(
            this.meta,
            new Content.From(
                String.join(
                    "\n",
                    "{",
                    "\"dist-tags\": {",
                    "    \"latest\": \"1.0.3\",",
                    "    \"first\": \"1.0.1\"",
                    "  }",
                    "}"
                ).getBytes(StandardCharsets.UTF_8)
            )
        ).join();
    }

    @Test
    void returnsOkAndUpdatesTags() {
        MatcherAssert.assertThat(
            "Response status is OK",
            new AddDistTagsSlice(this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(
                    RqMethod.GET, "/-/package/@hello%2fsimple-npm-project/dist-tags/second"
                ),
                Headers.EMPTY,
                new Content.From("1.0.2".getBytes(StandardCharsets.UTF_8))
            )
        );
        MatcherAssert.assertThat(
            "Meta.json is updated",
            new PublisherAs(this.storage.value(this.meta).join()).asciiString()
                .toCompletableFuture().join(),
            new IsEqual<>(
                "{\"dist-tags\":{\"latest\":\"1.0.3\",\"first\":\"1.0.1\",\"second\":\"1.0.2\"}}"
            )
        );
    }

    @Test
    void returnsNotFoundIfMetaIsNotFound() {
        MatcherAssert.assertThat(
            new AddDistTagsSlice(this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/-/package/@hello%2ftest-project/dist-tags/second")
            )
        );
    }

    @Test
    void returnsBadRequest() {
        MatcherAssert.assertThat(
            new AddDistTagsSlice(this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.GET, "/abc/123")
            )
        );
    }

}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.RsStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

/**
 * Test for {@link GetDistTagsSlice}.
 */
class GetDistTagsSliceTest {

    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.storage.save(
            new Key.From("@hello/simple-npm-project", "meta.json"),
            new Content.From(
                String.join(
                    "\n",
                    "{",
                    "\"dist-tags\": {",
                    "    \"latest\": \"1.0.3\",",
                    "    \"second\": \"1.0.2\",",
                    "    \"first\": \"1.0.1\"",
                    "  }",
                    "}"
                ).getBytes(StandardCharsets.UTF_8)
            )
        ).join();
    }

    @Test
    void readsDistTagsFromMeta() {
        Assertions.assertEquals(
            "{\"latest\":\"1.0.3\",\"second\":\"1.0.2\",\"first\":\"1.0.1\"}",
            new GetDistTagsSlice(this.storage).response(
                new RequestLine(RqMethod.GET, "/-/package/@hello%2fsimple-npm-project/dist-tags"),
                Headers.EMPTY, Content.EMPTY
            ).join().body().asString()
        );
    }

    @Test
    void returnsNotFoundIfMetaIsNotFound() {
        Assertions.assertEquals(
            RsStatus.NOT_FOUND,
            new GetDistTagsSlice(this.storage).response(
                new RequestLine(RqMethod.GET, "/-/package/@hello%2fanother-npm-project/dist-tags"),
                Headers.EMPTY, Content.EMPTY
            ).join().status()
        );
    }

}

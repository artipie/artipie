/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.slice.KeyFromPath;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

/**
 * Test for {@link LatestSlice}.
 */
public class LatestSliceTest {

    @Test
    void returnsLatestVersion() throws ExecutionException, InterruptedException {
        final Storage storage = new InMemoryStorage();
        storage.save(
            new KeyFromPath("example.com/latest/news/@v/v0.0.1.zip"), new Content.From(new byte[]{})
        ).get();
        storage.save(
            new KeyFromPath("example.com/latest/news/@v/v0.0.1.mod"), new Content.From(new byte[]{})
        ).get();
        storage.save(
            new KeyFromPath("example.com/latest/news/@v/v0.0.1.info"),
            new Content.From(new byte[]{})
        ).get();
        storage.save(
            new KeyFromPath("example.com/latest/news/@v/v0.0.2.zip"), new Content.From(new byte[]{})
        ).get();
        storage.save(
            new KeyFromPath("example.com/latest/news/@v/v0.0.2.mod"), new Content.From(new byte[]{})
        ).get();
        final String info = "{\"Version\":\"v0.0.2\",\"Time\":\"2019-06-28T10:22:31Z\"}";
        storage.save(
            new KeyFromPath("example.com/latest/news/@v/v0.0.2.info"),
            new Content.From(info.getBytes())
        ).get();
        Response response = new LatestSlice(storage).response(
            RequestLine.from("GET example.com/latest/news/@latest?a=b HTTP/1.1"),
            Headers.EMPTY, Content.EMPTY
        ).join();
        Assertions.assertArrayEquals(info.getBytes(), response.body().asBytes());
        MatcherAssert.assertThat(
            response.headers(),
            Matchers.containsInRelativeOrder(ContentType.json())
        );
    }

    @Test
    void returnsNotFondWhenModuleNotFound() {
        Response response = new LatestSlice(new InMemoryStorage()).response(
            RequestLine.from("GET example.com/first/@latest HTTP/1.1"), Headers.EMPTY, Content.EMPTY
        ).join();
        Assertions.assertEquals(RsStatus.NOT_FOUND, response.status());
    }

}

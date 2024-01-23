/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.slice.KeyFromPath;
import io.reactivex.Flowable;
import java.util.concurrent.ExecutionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link LatestSlice}.
 * @since 0.3
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
        MatcherAssert.assertThat(
            new LatestSlice(storage).response(
                "GET example.com/latest/news/@latest?a=b HTTP/1.1", Headers.EMPTY, Flowable.empty()
            ),
            Matchers.allOf(
                new RsHasBody(info.getBytes()),
                new RsHasHeaders(new Header("content-type", "application/json"))
            )
        );
    }

    @Test
    void returnsNotFondWhenModuleNotFound() {
        MatcherAssert.assertThat(
            new LatestSlice(new InMemoryStorage()).response(
                "GET example.com/first/@latest HTTP/1.1", Headers.EMPTY, Flowable.empty()
            ),
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }

}

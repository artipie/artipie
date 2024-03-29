/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm.http;

import com.artipie.ArtipieException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.helm.ChartYaml;
import com.artipie.helm.metadata.IndexYamlMapping;
import com.artipie.http.Headers;
import com.artipie.http.ResponseImpl;
import com.artipie.http.hm.ResponseAssert;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.RsStatus;
import com.google.common.base.Throwables;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test case for {@link DownloadIndexSlice}.
 */
final class DownloadIndexSliceTest {

    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://central.artipie.com/", "http://central.artipie.com"})
    void returnsOkAndUpdateEntriesUrlsForBaseWithOrWithoutTrailingSlash(final String base) {
//        final AtomicReference<String> cbody = new AtomicReference<>();
//        final AtomicReference<RsStatus> cstatus = new AtomicReference<>();
        new TestResource("index.yaml").saveTo(this.storage);

        ResponseImpl resp = new DownloadIndexSlice(base, this.storage)
            .response(new RequestLine(RqMethod.GET, "/index.yaml"),
                Headers.EMPTY, Content.EMPTY)
            .join();
        ResponseAssert.checkOk(resp);
        MatcherAssert.assertThat(
            "Uri was corrected modified",
            new ChartYaml(
                new IndexYamlMapping(resp.body().asString())
                    .byChart("tomcat").get(0)
            ).urls().get(0),
            new IsEqual<>(String.format("%s/tomcat-0.4.1.tgz", base.replaceAll("/$", "")))
        );
    }

    @Test
    void returnsBadRequest() {
        MatcherAssert.assertThat(
            new DownloadIndexSlice("http://localhost:8080", this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.GET, "/bad/request")
            )
        );
    }

    @Test
    void returnsNotFound() {
        MatcherAssert.assertThat(
            new DownloadIndexSlice("http://localhost:8080", this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.GET, "/index.yaml")
            )
        );
    }

    @Test
    void throwsMalformedUrlExceptionForInvalidBase() {
        final String base = "withoutschemelocalhost:8080";
        final Throwable thr = Assertions.assertThrows(
            ArtipieException.class,
            () -> new DownloadIndexSlice(base, this.storage)
        );
        MatcherAssert.assertThat(
            Throwables.getRootCause(thr),
            new IsInstanceOf(MalformedURLException.class)
        );
    }

    @Test
    void throwsExceptionForInvalidUriFromIndexYaml() {
        final String base = "http://localhost:8080";
        final AtomicReference<Throwable> exc = new AtomicReference<>();
        new TestResource("index/invalid_uri.yaml")
            .saveTo(this.storage, new Key.From("index.yaml"));
        new DownloadIndexSlice(base, this.storage)
            .response(
                new RequestLine(RqMethod.GET, "/index.yaml"),
                Headers.EMPTY,
                Content.EMPTY
            ).handle(
                (res, thr) -> {
                    exc.set(thr);
                    return CompletableFuture.allOf();
                }
            ).join();
        MatcherAssert.assertThat(
            Throwables.getRootCause(exc.get()),
            new IsInstanceOf(URISyntaxException.class)
        );
    }
}

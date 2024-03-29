/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.ResponseImpl;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseAssert;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.security.policy.Policy;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

/**
 * Test for {@link PySlice}.
 */
class PySliceTest {

    /**
     * Test slice.
     */
    private PySlice slice;

    /**
     * Test storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.slice = new PySlice(
            this.storage, Policy.FREE,
            (username, password) -> Optional.empty(),
            "*", Optional.empty()
        );
    }

    @Test
    void returnsIndexPage() {
        final byte[] content = "python package".getBytes();
        final String key = "simple/simple-0.1-py3-cp33m-linux_x86.whl";
        this.storage.save(new Key.From(key), new Content.From(content)).join();
        ResponseImpl resp = this.slice.response(
            new RequestLine("GET", "/simple"),
            Headers.EMPTY,
            Content.EMPTY
        ).join();
        ResponseAssert.check(resp, RsStatus.OK,
            new Header("Content-type", "text/html; charset=utf-8"),
            new Header("Content-Length", "217")
        );
        MatcherAssert.assertThat(
            resp.body().asString(),
            new StringContains("simple-0.1-py3-cp33m-linux_x86.whl")
        );
    }

    @Test
    void returnsIndexPageByRootRequest() {
        final byte[] content = "python package".getBytes();
        final String key = "simple/alarmtime-0.1.5.tar.gz";
        this.storage.save(new Key.From(key), new Content.From(content)).join();
        ResponseImpl resp = this.slice.response(
            new RequestLine("GET", "/"),
            Headers.EMPTY,
            Content.EMPTY
        ).join();
        ResponseAssert.check(resp, RsStatus.OK,
            new Header("Content-type", "text/html; charset=utf-8"),
            new Header("Content-Length", "193")
        );
        MatcherAssert.assertThat(
            resp.body().asString(),
            new StringContains("alarmtime-0.1.5.tar.gz")
        );
    }

    @Test
    void redirectsToNormalizedPath() {
        ResponseAssert.check(
            this.slice.response(
                new RequestLine("GET", "/one/Two_three"),
                Headers.EMPTY,
                Content.EMPTY
            ).join(),
            RsStatus.MOVED_PERMANENTLY,
            new Header("Location", "/one/two-three")
        );
    }

    @Test
    void returnsBadRequestOnEmptyPost() {
        ResponseAssert.check(
            this.slice.response(
                new RequestLine("POST", "/sample.tar"),
                Headers.from("content-type", "multipart/form-data; boundary=\"abc123\""),
                Content.EMPTY
            ).join(),
            RsStatus.BAD_REQUEST
        );
    }

    @ParameterizedTest
    @CsvSource({
        "python zip package,my/zip/my-project.zip",
        "python tar package,my-tar/my-project.tar",
        "python package,my/my-project.tar.gz",
        "python tar z package,my/my-project-z.tar.Z",
        "python tar bz2 package,new/my/my-project.tar.bz2",
        "python wheel,my/my-project.whl",
        "python egg package,eggs/python-egg.egg"
    })
    void downloadsVariousArchives(final String content, final String key) {
        this.storage.save(new Key.From(key), new Content.From(content.getBytes())).join();
        ResponseAssert.check(
            this.slice.response(
                new RequestLine("GET", key),
                Headers.EMPTY,
                Content.EMPTY
            ).join(),
            RsStatus.OK,
            content.getBytes()
        );
    }

}

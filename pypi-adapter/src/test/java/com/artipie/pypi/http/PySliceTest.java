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
import com.artipie.http.headers.Authorization;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.IsHeader;
import com.artipie.http.hm.IsString;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link PySlice}.
 * @since 0.6
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
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
        this.slice = new PySlice(this.storage);
    }

    @Test
    void returnsIndexPage() {
        final byte[] content = "python package".getBytes();
        final String key = "simple/simple-0.1-py3-cp33m-linux_x86.whl";
        this.storage.save(new Key.From(key), new Content.From(content)).join();
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine("GET", "/simple").toString(),
                this.authorization(),
                Flowable.empty()
            ),
            Matchers.allOf(
                new RsHasBody(
                    new IsString(new StringContains("simple-0.1-py3-cp33m-linux_x86.whl"))
                ),
                new RsHasStatus(RsStatus.OK),
                new RsHasHeaders(
                    new Header("Content-type", "text/html"),
                    new Header("Content-Length", "217")
                )
            )
        );
    }

    @Test
    void returnsIndexPageByRootRequest() {
        final byte[] content = "python package".getBytes();
        final String key = "simple/alarmtime-0.1.5.tar.gz";
        this.storage.save(new Key.From(key), new Content.From(content)).join();
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine("GET", "/").toString(),
                this.authorization(),
                Flowable.empty()
            ),
            Matchers.allOf(
                new RsHasBody(
                    new IsString(new StringContains("alarmtime-0.1.5.tar.gz"))
                ),
                new RsHasStatus(RsStatus.OK),
                new RsHasHeaders(
                    new Header("Content-type", "text/html"),
                    new Header("Content-Length", "193")
                )
            )
        );
    }

    @Test
    void redirectsToNormalizedPath() {
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine("GET", "/one/Two_three").toString(),
                this.authorization(),
                Flowable.empty()
            ),
            new ResponseMatcher(
                RsStatus.MOVED_PERMANENTLY,
                new IsHeader("Location", "/one/two-three")
            )
        );
    }

    @Test
    void returnsBadRequestOnEmptyPost() {
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine("POST", "/sample.tar").toString(),
                new Headers.From(
                    this.authorization(),
                    "content-type", "multipart/form-data; boundary=\"abc123\""
                ),
                Flowable.empty()
            ),
            new RsHasStatus(RsStatus.BAD_REQUEST)
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
        MatcherAssert.assertThat(
            this.slice.response(
                new RequestLine("GET", key).toString(),
                this.authorization(),
                Flowable.empty()
            ),
            new ResponseMatcher(
                RsStatus.OK,
                content.getBytes()
            )
        );
    }

    /**
     * Gets Authorization header.
     *
     * @return Headers.
     */
    private Headers authorization() {
        return new Headers.From(new Authorization.Basic("usr", "pwd"));
    }

}

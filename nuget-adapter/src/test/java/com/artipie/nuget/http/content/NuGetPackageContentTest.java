/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http.content;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.nuget.AstoRepository;
import com.artipie.nuget.http.NuGet;
import com.artipie.nuget.http.TestAuthentication;
import com.artipie.security.policy.PolicyByUsername;
import io.reactivex.Flowable;
import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NuGet}.
 * Package Content resource.
 *
 * @since 0.1
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
class NuGetPackageContentTest {

    /**
     * Storage used in tests.
     */
    private Storage storage;

    /**
     * Tested NuGet slice.
     */
    private NuGet nuget;

    @BeforeEach
    void init() throws Exception {
        this.storage = new InMemoryStorage();
        this.nuget = new NuGet(
            URI.create("http://localhost").toURL(),
            new AstoRepository(this.storage),
            new PolicyByUsername(TestAuthentication.USERNAME),
            new TestAuthentication(),
            "test",
            Optional.empty()
        );
    }

    @Test
    void shouldGetPackageContent() {
        final byte[] data = "data".getBytes();
        new BlockingStorage(this.storage).save(
            new Key.From("package", "1.0.0", "content.nupkg"),
            data
        );
        MatcherAssert.assertThat(
            "Package content should be returned in response",
            this.nuget.response(
                new RequestLine(
                    RqMethod.GET,
                    "/content/package/1.0.0/content.nupkg"
                ).toString(),
                new TestAuthentication.Headers(),
                Flowable.empty()
            ),
            new AllOf<>(
                Arrays.asList(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(data)
                )
            )
        );
    }

    @Test
    void shouldFailGetPackageContentWhenNotExists() {
        MatcherAssert.assertThat(
            "Not existing content should not be found",
            this.nuget.response(
                new RequestLine(
                    RqMethod.GET,
                    "/content/package/1.0.0/logo.png"
                ).toString(),
                new TestAuthentication.Headers(),
                Flowable.empty()
            ),
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }

    @Test
    void shouldFailPutPackageContent() {
        final Response response = this.nuget.response(
            new RequestLine(
                RqMethod.PUT,
                "/content/package/1.0.0/content.nupkg"
            ).toString(),
            new TestAuthentication.Headers(),
            Flowable.empty()
        );
        MatcherAssert.assertThat(
            "Package content cannot be put",
            response,
            new RsHasStatus(RsStatus.METHOD_NOT_ALLOWED)
        );
    }

    @Test
    void shouldGetPackageVersions() {
        final byte[] data = "example".getBytes();
        new BlockingStorage(this.storage).save(
            new Key.From("package2", "index.json"),
            data
        );
        MatcherAssert.assertThat(
            this.nuget.response(
                new RequestLine(
                    RqMethod.GET,
                    "/content/package2/index.json"
                ).toString(),
                new TestAuthentication.Headers(),
                Flowable.empty()
            ),
            Matchers.allOf(
                new RsHasStatus(RsStatus.OK),
                new RsHasBody(data)
            )
        );
    }

    @Test
    void shouldFailGetPackageVersionsWhenNotExists() {
        MatcherAssert.assertThat(
            this.nuget.response(
                new RequestLine(
                    RqMethod.GET,
                    "/content/unknown-package/index.json"
                ).toString(),
                new TestAuthentication.Headers(),
                Flowable.empty()
            ),
            new RsHasStatus(RsStatus.NOT_FOUND)
        );
    }

    @Test
    void shouldUnauthorizedGetPackageContentByAnonymousUser() {
        MatcherAssert.assertThat(
            this.nuget.response(
                new RequestLine(
                    RqMethod.GET,
                    "/content/package/2.0.0/content.nupkg"
                ).toString(),
                Headers.EMPTY,
                Flowable.empty()
            ),
            new ResponseMatcher(RsStatus.UNAUTHORIZED, Headers.EMPTY)
        );
    }
}

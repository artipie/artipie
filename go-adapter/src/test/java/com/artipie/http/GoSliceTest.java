/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.Authentication;
import com.artipie.http.headers.Authorization;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.security.policy.Policy;
import com.artipie.security.policy.PolicyByUsername;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test for {@link GoSlice}.
 * @since 0.3
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class GoSliceTest {

    /**
     * Test user.
     */
    private static final Pair<String, String> USER = new ImmutablePair<>("Alladin", "openSesame");

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsInfo(final boolean anonymous) throws Exception {
        final String path = "news.info/some/day/@v/v0.1.info";
        final String body = "{\"Version\":\"0.1\",\"Time\":\"2020-01-24T00:54:14Z\"}";
        MatcherAssert.assertThat(
            this.slice(GoSliceTest.storage(path, body), anonymous),
            new SliceHasResponse(
                matchers(body, "application/json"), GoSliceTest.line(path),
                this.headers(anonymous), Content.EMPTY
            )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsMod(final boolean anonymous) throws Exception {
        final String path = "example.com/mod/one/@v/v1.mod";
        final String body = "bla-bla";
        MatcherAssert.assertThat(
            this.slice(GoSliceTest.storage(path, body), anonymous),
            new SliceHasResponse(
                matchers(body, "text/plain"), GoSliceTest.line(path),
                this.headers(anonymous), Content.EMPTY
            )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsZip(final boolean anonymous) throws Exception {
        final String path = "modules.zip/foo/bar/@v/v1.0.9.zip";
        final String body = "smth";
        MatcherAssert.assertThat(
            this.slice(GoSliceTest.storage(path, body), anonymous),
            new SliceHasResponse(
                matchers(body, "application/zip"), GoSliceTest.line(path),
                this.headers(anonymous), Content.EMPTY
            )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsList(final boolean anonymous) throws Exception {
        final String path = "example.com/list/bar/@v/list";
        final String body = "v1.2.3";
        MatcherAssert.assertThat(
            this.slice(GoSliceTest.storage(path, body), anonymous),
            new SliceHasResponse(
                matchers(body, "text/plain"), GoSliceTest.line(path),
                this.headers(anonymous), Content.EMPTY
            )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void fallbacks(final boolean anonymous) throws Exception {
        final String path = "example.com/abc/def";
        final String body = "v1.8.3";
        MatcherAssert.assertThat(
            this.slice(GoSliceTest.storage(path, body), anonymous),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND), GoSliceTest.line(path),
                this.headers(anonymous), Content.EMPTY
            )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsLatest(final boolean anonymous) throws Exception {
        final String body = "{\"Version\":\"1.1\",\"Time\":\"2020-01-24T00:54:14Z\"}";
        MatcherAssert.assertThat(
            this.slice(GoSliceTest.storage("example.com/latest/bar/@v/v1.1.info", body), anonymous),
            new SliceHasResponse(
                matchers(body, "application/json"),
                GoSliceTest.line("example.com/latest/bar/@latest"),
                this.headers(anonymous), Content.EMPTY
            )
        );
    }

    /**
     * Constructs {@link GoSlice}.
     * @param storage Storage
     * @param anonymous Is authorisation required?
     * @return Instance of {@link GoSlice}
     */
    private GoSlice slice(final Storage storage, final boolean anonymous) {
        if (anonymous) {
            return new GoSlice(storage, Policy.FREE, (name, pswd) -> Optional.of(AuthUser.ANONYMOUS), "test");
        }
        return new GoSlice(storage,
            new PolicyByUsername(USER.getKey()),
            new Authentication.Single(USER.getKey(), USER.getValue()),
            "test"
        );
    }

    private Headers headers(final boolean anonymous) {
        return anonymous ? Headers.EMPTY : new Headers.From(
            new Authorization.Basic(GoSliceTest.USER.getKey(), GoSliceTest.USER.getValue())
        );
    }

    /**
     * Composes matchers.
     * @param body Body
     * @param type Content-type
     * @return List of matchers
     */
    private static AllOf<Response> matchers(final String body,
        final String type) {
        return new AllOf<>(
            Stream.of(
                new RsHasBody(body.getBytes()),
                new RsHasHeaders(new Header("content-type", type))
            ).collect(Collectors.toList())
        );
    }

    /**
     * Request line.
     * @param path Path
     * @return Proper request line
     */
    private static RequestLine line(final String path) {
        return new RequestLine("GET", path);
    }

    /**
     * Composes storage.
     * @param path Where to store
     * @param body Body to store
     * @return Storage
     * @throws ExecutionException On error
     * @throws InterruptedException On error
     */
    private static Storage storage(final String path, final String body)
        throws ExecutionException, InterruptedException {
        final Storage storage = new InMemoryStorage();
        storage.save(
            new KeyFromPath(path),
            new Content.From(body.getBytes())
        ).get();
        return storage;
    }

}

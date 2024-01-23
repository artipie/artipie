/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.TokenAuthentication;
import com.artipie.http.auth.Tokens;
import com.artipie.http.headers.Authorization;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.NotImplementedException;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DeleteTokenSlice}.
 * @since 0.3
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DeleteTokenSliceTest {

    @Test
    void removesToken() {
        MatcherAssert.assertThat(
            "Incorrect response status, 201 CREATED is expected",
            new DeleteTokenSlice(new FakeTokens()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(RqMethod.DELETE, "/authentications$"),
                new Headers.From(new Authorization.Token("abc123")),
                Content.EMPTY
            )
        );
    }

    @Test
    void returnsBadRequestIfTokenIsNotFound() {
        MatcherAssert.assertThat(
            "Incorrect response status, BAD_REQUEST is expected",
            new DeleteTokenSlice(new FakeTokens()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.DELETE, "/authentications$"),
                new Headers.From(new Authorization.Token("any")),
                Content.EMPTY
            )
        );
    }

    @Test
    void returnsUnauthorizedIfHeaderIsNotPresent() {
        MatcherAssert.assertThat(
            "Incorrect response status, BAD_REQUEST is expected",
            new DeleteTokenSlice(new FakeTokens()),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.UNAUTHORIZED),
                new RequestLine(RqMethod.DELETE, "/authentications$"),
                Headers.EMPTY,
                Content.EMPTY
            )
        );
    }

    /**
     * Fake test implementation of {@link Tokens}.
     * @since 0.3
     */
    private static final class  FakeTokens implements Tokens {

        @Override
        public TokenAuthentication auth() {
            return tkn -> {
                Optional<AuthUser> res = Optional.empty();
                if (tkn.equals("abc123")) {
                    res = Optional.of(new AuthUser("Alice", "test"));
                }
                return CompletableFuture.completedFuture(res);
            };
        }

        @Override
        public String generate(final AuthUser user) {
            throw new NotImplementedException("Not implemented");
        }
    }

}

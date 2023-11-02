/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.TokenAuthentication;
import com.artipie.http.auth.Tokens;
import com.artipie.http.headers.Authorization;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import org.apache.commons.lang3.NotImplementedException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link GenerateTokenSlice}.
 * @since 0.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
class GenerateTokenSliceTest {

    /**
     * Test token.
     */
    private static final String TOKEN = "abc123";

    @Test
    void addsToken() {
        final String name = "Alice";
        final String pswd = "wonderland";
        MatcherAssert.assertThat(
            "Slice response in not 200 OK",
            new GenerateTokenSlice(
                new Authentication.Single(name, pswd),
                new FakeAuthTokens()
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(
                        String.format("{\"token\":\"%s\"}", GenerateTokenSliceTest.TOKEN).getBytes()
                    )
                ),
                new RequestLine(RqMethod.POST, "/authentications"),
                new Headers.From(new Authorization.Basic(name, pswd)),
                Content.EMPTY
            )
        );
    }

    @Test
    void returnsUnauthorized() {
        MatcherAssert.assertThat(
            new GenerateTokenSlice(
                new Authentication.Single("Any", "123"),
                new FakeAuthTokens()
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.UNAUTHORIZED),
                new RequestLine(RqMethod.POST, "/any/line")
            )
        );
    }

    /**
     * Fake implementation of {@link Tokens}.
     * @since 0.5
     */
    static class FakeAuthTokens implements Tokens {

        @Override
        public TokenAuthentication auth() {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public String generate(final AuthUser user) {
            return GenerateTokenSliceTest.TOKEN;
        }
    }

}

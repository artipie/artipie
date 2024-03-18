/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem;

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.gem.http.GemSlice;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.Authentication;
import com.artipie.http.headers.Authorization;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.perms.EmptyPermissions;
import com.artipie.security.policy.Policy;
import com.artipie.security.policy.PolicyByUsername;
import io.reactivex.Flowable;
import org.cactoos.text.Base64Encoded;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.PermissionCollection;
import java.util.Arrays;
import java.util.Optional;

/**
 * A test for api key endpoint.
 *
 * @since 0.3
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class AuthTest {

    @Test
    public void keyIsReturned() {
        final String token = "aGVsbG86d29ybGQ=";
        final Headers headers = Headers.from(
            new Authorization(String.format("Basic %s", token))
        );
        MatcherAssert.assertThat(
            new GemSlice(
                new InMemoryStorage(),
                Policy.FREE,
                (name, pwd) -> Optional.of(new AuthUser("user", "test")),
                ""
            ).response(
                new RequestLine("GET", "/api/v1/api_key"),
                headers,
                Flowable.empty()
            ), new RsHasBody(token.getBytes(StandardCharsets.UTF_8))
        );
    }

    @Test
    public void unauthorizedWhenNoIdentity() {
        MatcherAssert.assertThat(
            new GemSlice(
                new InMemoryStorage(),
                Policy.FREE,
                (username, password) -> Optional.of(AuthUser.ANONYMOUS),
                ""
            ).response(
                new RequestLine("GET", "/api/v1/api_key"),
                Headers.EMPTY,
                Flowable.empty()
            ), new RsHasStatus(RsStatus.UNAUTHORIZED)
        );
    }

    @Test
    public void notAllowedToPushUsersAreRejected() throws IOException {
        final String lgn = "usr";
        final String pwd = "pwd";
        final String token = new Base64Encoded(String.format("%s:%s", lgn, pwd)).asString();
        final String repo = "test";
        MatcherAssert.assertThat(
            new GemSlice(
                new InMemoryStorage(),
                user -> {
                    final PermissionCollection res;
                    if (user.name().equals(lgn)) {
                        final AdapterBasicPermission perm =
                            new AdapterBasicPermission(repo, Action.Standard.READ);
                        res = perm.newPermissionCollection();
                        res.add(perm);
                    } else {
                        res = EmptyPermissions.INSTANCE;
                    }
                    return res;
                },
                new Authentication.Single(lgn, pwd),
                repo
            ).response(
                new RequestLine("POST", "/api/v1/gems"),
                Headers.from(new Authorization(token)),
                Flowable.empty()
            ), new RsHasStatus(RsStatus.FORBIDDEN)
        );
    }

    @Test
    public void notAllowedToInstallsUsersAreRejected() throws IOException {
        final String lgn = "usr";
        final String pwd = "pwd";
        final String token = new Base64Encoded(String.format("%s:%s", lgn, pwd)).asString();
        MatcherAssert.assertThat(
            new GemSlice(
                new InMemoryStorage(),
                new PolicyByUsername("another user"),
                new Authentication.Single(lgn, pwd),
                "test"
            ).response(
                new RequestLine("GET", "specs.4.8"),
                Headers.from(new Authorization(token)),
                Flowable.empty()
            ), new RsHasStatus(RsStatus.FORBIDDEN)
        );
    }

    @Test
    public void returnsUnauthorizedIfUnableToAuthenticate() throws IOException {
        MatcherAssert.assertThat(
            AuthTest.postWithBasicAuth(false),
            new AllOf<>(
                Arrays.asList(
                    new RsHasStatus(RsStatus.UNAUTHORIZED),
                    new RsHasHeaders(new Header("WWW-Authenticate", "Basic realm=\"artipie\""))
                )
            )
        );
    }

    @Test
    public void returnsOkWhenBasicAuthTokenCorrect() throws IOException {
        MatcherAssert.assertThat(
            AuthTest.postWithBasicAuth(true),
            new RsHasStatus(RsStatus.CREATED)
        );
    }

    private static Response postWithBasicAuth(final boolean authorized) throws IOException {
        final String user = "alice";
        final String pswd = "123";
        final String token;
        if (authorized) {
            token = new Base64Encoded(String.format("%s:%s", user, pswd)).asString();
        } else {
            token = new Base64Encoded(String.format("%s:wrong%s", user, pswd)).asString();
        }
        return new GemSlice(
            new InMemoryStorage(),
            new PolicyByUsername(user),
            new Authentication.Single(user, pswd),
            "test"
        ).response(
            new RequestLine("POST", "/api/v1/gems"),
            Headers.from(new Authorization("Basic " + token)),
            Flowable.just(
                ByteBuffer.wrap(new TestResource("rails-6.0.2.2.gem").asBytes())
            )
        );
    }
}

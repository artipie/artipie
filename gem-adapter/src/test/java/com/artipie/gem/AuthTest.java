/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem;

import com.artipie.asto.Content;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.gem.http.GemSlice;
import com.artipie.http.Headers;
import com.artipie.http.ResponseImpl;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.Authentication;
import com.artipie.http.headers.Authorization;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.perms.EmptyPermissions;
import com.artipie.security.policy.Policy;
import com.artipie.security.policy.PolicyByUsername;
import org.cactoos.text.Base64Encoded;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.PermissionCollection;
import java.util.Arrays;
import java.util.Optional;

/**
 * A test for api key endpoint.
 */
public class AuthTest {

    @Test
    public void keyIsReturned() {
        final String token = "aGVsbG86d29ybGQ=";
        final Headers headers = Headers.from(new Authorization("Basic " + token));
        Assertions.assertEquals(
            token,
            new GemSlice(
                new InMemoryStorage(),
                Policy.FREE,
                (name, pwd) -> Optional.of(new AuthUser("user", "test")),
                ""
            ).response(
                new RequestLine("GET", "/api/v1/api_key"), headers, Content.EMPTY
            ).join().body().asString()
        );
    }

    @Test
    public void unauthorizedWhenNoIdentity() {
        Assertions.assertEquals(
            RsStatus.UNAUTHORIZED,
            new GemSlice(
                new InMemoryStorage(),
                Policy.FREE,
                (username, password) -> Optional.of(AuthUser.ANONYMOUS),
                ""
            ).response(
                new RequestLine("GET", "/api/v1/api_key"), Headers.EMPTY, Content.EMPTY
            ).join().status()
        );
    }

    @Test
    public void notAllowedToPushUsersAreRejected() throws IOException {
        final String lgn = "usr";
        final String pwd = "pwd";
        final String token = new Base64Encoded(String.format("%s:%s", lgn, pwd)).asString();
        final String repo = "test";
        Assertions.assertEquals(
            RsStatus.FORBIDDEN,
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
                Content.EMPTY
            ).join().status()
        );
    }

    @Test
    public void notAllowedToInstallsUsersAreRejected() throws IOException {
        final String lgn = "usr";
        final String pwd = "pwd";
        final String token = new Base64Encoded(String.format("%s:%s", lgn, pwd)).asString();
        Assertions.assertEquals(
            RsStatus.FORBIDDEN,
            new GemSlice(
                new InMemoryStorage(),
                new PolicyByUsername("another user"),
                new Authentication.Single(lgn, pwd),
                "test"
            ).response(
                new RequestLine("GET", "specs.4.8"),
                Headers.from(new Authorization(token)),
                Content.EMPTY
            ).join().status()
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

    private static ResponseImpl postWithBasicAuth(final boolean authorized) throws IOException {
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
            new Content.From(new TestResource("rails-6.0.2.2.gem").asBytes())
        ).join();
    }
}

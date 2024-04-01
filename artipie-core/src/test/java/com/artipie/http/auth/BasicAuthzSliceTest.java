/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.RsStatus;
import com.artipie.http.headers.Authorization;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseAssert;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.slice.SliceSimple;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.perms.EmptyPermissions;
import com.artipie.security.policy.Policy;
import com.artipie.security.policy.PolicyByUsername;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Test for {@link BasicAuthzSlice}.
 */
class BasicAuthzSliceTest {

    @Test
    void proxyToOriginSliceIfAllowed() {
        final String user = "test_user";
        ResponseAssert.check(
            new BasicAuthzSlice(
                (rqline, headers, body) -> CompletableFuture.completedFuture(
                    ResponseBuilder.ok().headers(headers).build()),
                (usr, pwd) -> Optional.of(new AuthUser(user, "test")),
                new OperationControl(
                    Policy.FREE,
                    new AdapterBasicPermission("any_repo_name", Action.ALL)
                )
            ).response(
                new RequestLine("GET", "/foo"),
                Headers.from(new Authorization.Basic(user, "pwd")),
                Content.EMPTY
            ).join(),
            RsStatus.OK, new Header(AuthzSlice.LOGIN_HDR, user));
    }

    @Test
    void returnsUnauthorizedErrorIfCredentialsAreWrong() {
        ResponseAssert.check(
            new BasicAuthzSlice(
                new SliceSimple(ResponseBuilder.ok().build()),
                (user, pswd) -> Optional.empty(),
                new OperationControl(
                    user -> EmptyPermissions.INSTANCE,
                    new AdapterBasicPermission("any", Action.NONE)
                )
            ).response(
                new RequestLine("POST", "/bar", "HTTP/1.2"),
                Headers.from(new Authorization.Basic("aaa", "bbbb")),
                Content.EMPTY
            ).join(),
            RsStatus.UNAUTHORIZED, new Header("WWW-Authenticate", "Basic realm=\"artipie\"")
        );
    }

    @Test
    void returnsForbiddenIfNotAllowed() {
        final String name = "john";
        ResponseAssert.check(
            new BasicAuthzSlice(
                new SliceSimple(ResponseBuilder.ok().build()),
                (user, pswd) -> Optional.of(new AuthUser(name)),
                new OperationControl(
                    user -> EmptyPermissions.INSTANCE,
                    new AdapterBasicPermission("any", Action.NONE)
                )
            ).response(
                new RequestLine("DELETE", "/baz", "HTTP/1.3"),
                Headers.from(new Authorization.Basic(name, "123")),
                Content.EMPTY
            ).join(),
            RsStatus.FORBIDDEN
        );
    }

    @Test
    void returnsUnauthorizedForAnonymousUser() {
        ResponseAssert.check(
            new BasicAuthzSlice(
                new SliceSimple(ResponseBuilder.ok().build()),
                (user, pswd) -> Assertions.fail("Shouldn't be called"),
                new OperationControl(
                    user -> {
                        MatcherAssert.assertThat(
                            user.name(),
                            Matchers.anyOf(Matchers.is("anonymous"), Matchers.is("*"))
                        );
                        return EmptyPermissions.INSTANCE;
                    },
                    new AdapterBasicPermission("any", Action.NONE)
                )
            ).response(
                new RequestLine("DELETE", "/baz", "HTTP/1.3"),
                Headers.from(new Header("WWW-Authenticate", "Basic realm=\"artipie\"")),
                Content.EMPTY
            ).join(),
            RsStatus.UNAUTHORIZED
        );
    }

    @Test
    void parsesHeaders() {
        final String aladdin = "Aladdin";
        final String pswd = "open sesame";
        ResponseAssert.check(
            new BasicAuthzSlice(
                (rqline, headers, body) -> CompletableFuture.completedFuture(
                    ResponseBuilder.ok().headers(headers).build()),
                new Authentication.Single(aladdin, pswd),
                new OperationControl(
                    new PolicyByUsername(aladdin),
                    new AdapterBasicPermission("any", Action.ALL)
                )
            ).response(
                new RequestLine("PUT", "/my-endpoint"),
                Headers.from(new Authorization.Basic(aladdin, pswd)),
                Content.EMPTY
            ).join(),
            RsStatus.OK, new Header(AuthzSlice.LOGIN_HDR, "Aladdin")
        );
    }
}

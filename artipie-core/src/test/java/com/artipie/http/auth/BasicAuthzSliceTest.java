/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.auth;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.headers.Authorization;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.SliceSimple;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.perms.EmptyPermissions;
import com.artipie.security.policy.Policy;
import com.artipie.security.policy.PolicyByUsername;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link BasicAuthzSlice}.
 * @since 1.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class BasicAuthzSliceTest {

    @Test
    void proxyToOriginSliceIfAllowed() {
        MatcherAssert.assertThat(
            new BasicAuthzSlice(
                (rqline, headers, body) -> new RsWithHeaders(StandardRs.OK, headers),
                (user, pswd) -> Optional.empty(),
                new OperationControl(Policy.FREE, new AdapterBasicPermission("any", Action.ALL))
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasHeaders(
                        new Header(AuthzSlice.LOGIN_HDR, Authentication.ANY_USER.name())
                    )
                ),
                new RequestLine("GET", "/foo")
            )
        );
    }

    @Test
    void returnsUnauthorizedErrorIfCredentialsAreWrong() {
        MatcherAssert.assertThat(
            new BasicAuthzSlice(
                new SliceSimple(StandardRs.OK),
                (user, pswd) -> Optional.empty(),
                new OperationControl(
                    user -> EmptyPermissions.INSTANCE,
                    new AdapterBasicPermission("any", Action.NONE)
                )
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.UNAUTHORIZED),
                    new RsHasHeaders(new Header("WWW-Authenticate", "Basic realm=\"artipie\""))
                ),
                new Headers.From(new Authorization.Basic("aaa", "bbbb")),
                new RequestLine("POST", "/bar", "HTTP/1.2")
            )
        );
    }

    @Test
    void returnsForbiddenIfNotAllowed() {
        final String name = "john";
        MatcherAssert.assertThat(
            new BasicAuthzSlice(
                new SliceSimple(new RsWithStatus(RsStatus.OK)),
                (user, pswd) -> Optional.of(new AuthUser(name)),
                new OperationControl(
                    user -> EmptyPermissions.INSTANCE,
                    new AdapterBasicPermission("any", Action.NONE)
                )
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.FORBIDDEN),
                new RequestLine("DELETE", "/baz", "HTTP/1.3"),
                new Headers.From(new Authorization.Basic(name, "123")),
                Content.EMPTY
            )
        );
    }

    @Test
    void returnsForbiddenForAnonymousUser() {
        MatcherAssert.assertThat(
            new BasicAuthzSlice(
                new SliceSimple(new RsWithStatus(RsStatus.OK)),
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
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.FORBIDDEN),
                new RequestLine("DELETE", "/baz", "HTTP/1.3"),
                new Headers.From(new Header("WWW-Authenticate", "Basic realm=\"artipie\"")),
                Content.EMPTY
            )
        );
    }

    @Test
    void parsesHeaders() {
        final String aladdin = "Aladdin";
        final String pswd = "open sesame";
        MatcherAssert.assertThat(
            new BasicAuthzSlice(
                (rqline, headers, body) -> new RsWithHeaders(StandardRs.OK, headers),
                new Authentication.Single(aladdin, pswd),
                new OperationControl(
                    new PolicyByUsername(aladdin),
                    new AdapterBasicPermission("any", Action.ALL)
                )
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasHeaders(new Header(AuthzSlice.LOGIN_HDR, "Aladdin"))
                ),
                new RequestLine("PUT", "/my-endpoint"),
                new Headers.From(new Authorization.Basic(aladdin, pswd)),
                Content.EMPTY
            )
        );
    }

    @Test
    void doesNotAuthenticateIfNotNeeded() {
        MatcherAssert.assertThat(
            new BasicAuthzSlice(
                new SliceSimple(StandardRs.OK),
                (user, pswd) -> {
                    throw new IllegalStateException("Should not be invoked");
                },
                new OperationControl(
                    new PolicyByUsername(Authentication.ANY_USER.name()),
                    new AdapterBasicPermission("any", Action.ALL)
                )
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.GET, "/resource"),
                new Headers.From(new Authorization.Basic("alice", "12345")),
                Content.EMPTY
            )
        );
    }

}

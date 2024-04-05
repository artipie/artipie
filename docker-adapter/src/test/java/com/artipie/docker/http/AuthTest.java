/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Digest;
import com.artipie.docker.Docker;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.asto.TrustedBlobSource;
import com.artipie.docker.perms.DockerActions;
import com.artipie.docker.perms.DockerRegistryPermission;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.docker.perms.RegistryCategory;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.RsStatus;
import com.artipie.http.Slice;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.auth.BearerAuthScheme;
import com.artipie.http.headers.Authorization;
import com.artipie.http.hm.ResponseAssert;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.security.perms.EmptyPermissions;
import com.artipie.security.policy.Policy;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.shaded.com.google.common.collect.Sets;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Tests for {@link DockerSlice}.
 * Authentication & authorization tests.
 */
public final class AuthTest {

    private Docker docker;

    @BeforeEach
    void setUp() {
        this.docker = new AstoDocker("test_registry", new InMemoryStorage());
    }

    @ParameterizedTest
    @MethodSource("setups")
    void shouldUnauthorizedForAnonymousUser(final Method method, final RequestLine line) {
        ResponseAssert.check(
            method.slice(
                new TestPolicy(
                    new DockerRepositoryPermission("*", "whatever", DockerActions.PULL.mask())
                )
            ).response(line, Headers.EMPTY, Content.EMPTY).join(),
            RsStatus.UNAUTHORIZED
        );
    }

    @ParameterizedTest
    @MethodSource("setups")
    void shouldReturnUnauthorizedWhenUserIsUnknown(final Method method, final RequestLine line) {
        ResponseAssert.check(
            method.slice(new DockerRepositoryPermission("*", "whatever", DockerActions.PULL.mask()))
                .response(line, method.headers(new TestAuthentication.User("chuck", "letmein")), Content.EMPTY)
                .join(),
            RsStatus.UNAUTHORIZED
        );
    }

    @ParameterizedTest
    @MethodSource("setups")
    void shouldReturnForbiddenWhenUserHasNoRequiredPermissions(
        final Method method,
        final RequestLine line,
        final Permission permission
    ) {
        ResponseAssert.check(
            method.slice(permission)
                .response(line, method.headers(TestAuthentication.BOB), Content.EMPTY)
                .join(),
            RsStatus.FORBIDDEN
        );
    }

    @Test
    @Disabled
    void shouldReturnForbiddenWhenUserHasNoRequiredPermissionOnSecondManifestPut() {
        final Basic basic = new Basic(this.docker);
        final RequestLine line = new RequestLine(RqMethod.PUT, "/v2/my-alpine/manifests/latest");
        final DockerRepositoryPermission permission =
            new DockerRepositoryPermission("*", "my-alpine", DockerActions.PUSH.mask());
        basic.slice(permission).response(
            line,
            basic.headers(TestAuthentication.ALICE),
            this.manifest()
        );
        MatcherAssert.assertThat(
            basic.slice(permission),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.FORBIDDEN),
                line,
                basic.headers(TestAuthentication.ALICE),
                Content.EMPTY
            )
        );
    }

    @Test
    void shouldReturnForbiddenWhenUserHasNoRequiredPermissionOnFirstManifestPut() {
        final Basic basic = new Basic(this.docker);
        final RequestLine line = new RequestLine(RqMethod.PUT, "/v2/my-alpine/manifests/latest");
        final DockerRepositoryPermission permission =
            new DockerRepositoryPermission("*", "my-alpine", DockerActions.PULL.mask());
        MatcherAssert.assertThat(
            basic.slice(permission),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.FORBIDDEN),
                line,
                basic.headers(TestAuthentication.ALICE),
                new Content.From(this.manifest())
            )
        );
    }

    @ParameterizedTest
    @MethodSource("setups")
    void shouldNotReturnUnauthorizedOrForbiddenWhenUserHasPermissions(
        Method method, RequestLine line, Permission permission
    ) {
        final Response response = method.slice(permission).response(
            line, method.headers(TestAuthentication.ALICE), Content.EMPTY
        ).join();
        Assertions.assertNotEquals(RsStatus.FORBIDDEN, response.status());
        Assertions.assertNotEquals(RsStatus.UNAUTHORIZED, response.status());
    }

    @ParameterizedTest
    @MethodSource("setups")
    void shouldOkWhenAnonymousUserHasPermissions(
        final Method method,
        final RequestLine line,
        final Permission permission
    ) {
        final Response response = method.slice(new TestPolicy(permission, "anonymous", "Alice"))
            .response(line, Headers.EMPTY, Content.EMPTY).join();
        MatcherAssert.assertThat(
            response,
            new AllOf<>(
                Arrays.asList(
                    new IsNot<>(new RsHasStatus(RsStatus.FORBIDDEN)),
                    new IsNot<>(new RsHasStatus(RsStatus.UNAUTHORIZED))
                )
            )
        );
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static Stream<Arguments> setups() {
        return Stream.of(new Basic(), new Bearer()).flatMap(AuthTest::setups);
    }

    /**
     * Create manifest content.
     *
     * @return Manifest content.
     */
    private Content manifest() {
        final byte[] content = "config".getBytes();
        final Digest digest = this.docker.repo("my-alpine").layers()
            .put(new TrustedBlobSource(content)).join();
        final byte[] data = String.format(
            "{\"config\":{\"digest\":\"%s\"},\"layers\":[],\"mediaType\":\"my-type\"}",
            digest.string()
        ).getBytes();
        return new Content.From(data);
    }

    private static Stream<Arguments> setups(final Method method) {
        return Stream.of(
            Arguments.of(
                method,
                new RequestLine(RqMethod.GET, "/v2/"),
                new DockerRegistryPermission("*", RegistryCategory.ALL.mask())
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.HEAD, "/v2/my-alpine/manifests/1"),
                new DockerRepositoryPermission("*", "my-alpine", DockerActions.PULL.mask())
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.GET, "/v2/my-alpine/manifests/2"),
                new DockerRepositoryPermission("*", "my-alpine", DockerActions.PULL.mask())
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.PUT, "/v2/my-alpine/manifests/latest"),
                new DockerRepositoryPermission("*", "my-alpine", DockerActions.PUSH.mask())
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.GET, "/v2/my-alpine/tags/list"),
                new DockerRepositoryPermission("*", "my-alpine", DockerActions.PULL.mask())
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.HEAD, "/v2/my-alpine/blobs/sha256:123"),
                new DockerRepositoryPermission("*", "my-alpine", DockerActions.PULL.mask())
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.GET, "/v2/my-alpine/blobs/sha256:012345"),
                new DockerRepositoryPermission("*", "my-alpine", DockerActions.PULL.mask())
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.POST, "/v2/my-alpine/blobs/uploads/"),
                new DockerRepositoryPermission("*", "my-alpine", DockerActions.PUSH.mask())
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.PATCH, "/v2/my-alpine/blobs/uploads/123"),
                new DockerRepositoryPermission("*", "my-alpine", DockerActions.PUSH.mask())
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.PUT, "/v2/my-alpine/blobs/uploads/12345"),
                new DockerRepositoryPermission("*", "my-alpine", DockerActions.PUSH.mask())
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.GET, "/v2/my-alpine/blobs/uploads/112233"),
                new DockerRepositoryPermission("*", "my-alpine", DockerActions.PULL.mask())
            )
        );
    }

    /**
     * Authentication method.
     */
    private interface Method {

        default Slice slice(final Permission perm) {
            return this.slice(new TestPolicy(perm));
        }

        Slice slice(Policy<PermissionCollection> policy);

        Headers headers(TestAuthentication.User user);

    }

    /**
     * Basic authentication method.
     *
     * @since 0.8
     */
    private static final class Basic implements Method {

        /**
         * Docker repo.
         */
        private final Docker docker;

        private Basic(final Docker docker) {
            this.docker = docker;
        }

        private Basic() {
            this(new AstoDocker("test_registry", new InMemoryStorage()));
        }

        @Override
        public Slice slice(final Policy<PermissionCollection> policy) {
            return new DockerSlice(
                this.docker,
                policy,
                new BasicAuthScheme(new TestAuthentication()),
                Optional.empty()
            );
        }

        @Override
        public Headers headers(final TestAuthentication.User user) {
            return user.headers();
        }

        @Override
        public String toString() {
            return "Basic";
        }
    }

    /**
     * Bearer authentication method.
     *
     * @since 0.8
     */
    private static final class Bearer implements Method {

        @Override
        public Slice slice(final Policy<PermissionCollection> policy) {
            return new DockerSlice(
                new AstoDocker("registry", new InMemoryStorage()),
                policy,
                new BearerAuthScheme(
                    token -> CompletableFuture.completedFuture(
                        Stream.of(TestAuthentication.ALICE, TestAuthentication.BOB)
                            .filter(user -> token.equals(token(user)))
                            .map(user -> new AuthUser(user.name(), "test"))
                            .findFirst()
                    ),
                    ""
                ),
                Optional.empty()
            );
        }

        @Override
        public Headers headers(final TestAuthentication.User user) {
            return Headers.from(new Authorization.Bearer(token(user)));
        }

        @Override
        public String toString() {
            return "Bearer";
        }

        private static String token(final TestAuthentication.User user) {
            return String.format("%s:%s", user.name(), user.password());
        }
    }

    static final class TestPolicy implements Policy<PermissionCollection> {

        private final Permission perm;
        private final Set<String> users;

        TestPolicy(final Permission perm) {
            this.perm = perm;
            this.users = Collections.singleton("Alice");
        }

        TestPolicy(final Permission perm, final String... users) {
            this.perm = perm;
            this.users = Sets.newHashSet(users);
        }

        @Override
        public PermissionCollection getPermissions(final AuthUser user) {
            final PermissionCollection res;
            if (this.users.contains(user.name())) {
                res = this.perm.newPermissionCollection();
                res.add(this.perm);
            } else {
                res = EmptyPermissions.INSTANCE;
            }
            return res;
        }
    }
}

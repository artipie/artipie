/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Blob;
import com.artipie.docker.Docker;
import com.artipie.docker.RepoName;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.asto.TrustedBlobSource;
import com.artipie.docker.perms.DockerActions;
import com.artipie.docker.perms.DockerRegistryPermission;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.docker.perms.RegistryCategory;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.auth.BearerAuthScheme;
import com.artipie.http.headers.Authorization;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseMatcher;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.security.perms.EmptyPermissions;
import com.artipie.security.policy.Policy;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsNot;
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
 *
 * @todo #434:30min test `shouldReturnForbiddenWhenUserHasNoRequiredPermissionOnSecondManifestPut`
 *  fails in github actions, locally it works fine. Figure out what is the problem and fix it.
 */
public final class AuthTest {

    private Docker docker;

    @BeforeEach
    void setUp() {
        this.docker = new AstoDocker(new InMemoryStorage());
    }

    @ParameterizedTest
    @MethodSource("setups")
    void shouldUnauthorizedForAnonymousUser(final Method method, final RequestLine line) {
        MatcherAssert.assertThat(
            method.slice(
                new TestPolicy(
                    new DockerRepositoryPermission("*", "whatever", DockerActions.PULL.mask())
                )
            ).response(line, Headers.EMPTY, Content.EMPTY),
            new IsUnauthorizedResponse()
        );
    }

    @ParameterizedTest
    @MethodSource("setups")
    void shouldReturnUnauthorizedWhenUserIsUnknown(final Method method, final RequestLine line) {
        MatcherAssert.assertThat(
            method.slice(
                new DockerRepositoryPermission("*", "whatever", DockerActions.PULL.mask())
            ).response(
                line,
                method.headers(new TestAuthentication.User("chuck", "letmein")),
                Content.EMPTY
            ),
            new IsUnauthorizedResponse()
        );
    }

    @ParameterizedTest
    @MethodSource("setups")
    void shouldReturnForbiddenWhenUserHasNoRequiredPermissions(
        final Method method,
        final RequestLine line,
        final Permission permission
    ) {
        MatcherAssert.assertThat(
            method.slice(permission).response(
                line,
                method.headers(TestAuthentication.BOB),
                Content.EMPTY
            ),
            new IsDeniedResponse()
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

    @Test
    void shouldOverwriteManifestIfAllowed() {
        final Basic basic = new Basic(this.docker);
        final String path = "/v2/my-alpine/manifests/abc";
        final RequestLine line = new RequestLine(RqMethod.PUT, path);
        final DockerRepositoryPermission permission =
            new DockerRepositoryPermission("*", "my-alpine", DockerActions.OVERWRITE.mask());
        Content manifest = this.manifest();
        MatcherAssert.assertThat(
            "Manifest was created for the first time",
            basic.slice(permission).response(
                line,
                basic.headers(TestAuthentication.ALICE),
                manifest
            ),
            new ResponseMatcher(
                RsStatus.CREATED,
                new Header("Location", path),
                new Header("Content-Length", "0"),
                new Header(
                    "Docker-Content-Digest",
                    "sha256:ef0ff2adcc3c944a63f7cafb386abc9a1d95528966085685ae9fab2a1c0bedbf"
                )
            )
        );
        MatcherAssert.assertThat(
            "Manifest was overwritten",
            basic.slice(permission).response(
                line,
                basic.headers(TestAuthentication.ALICE),
                manifest
            ),
            new ResponseMatcher(
                RsStatus.CREATED,
                new Header("Location", path),
                new Header("Content-Length", "0"),
                new Header(
                    "Docker-Content-Digest",
                    "sha256:ef0ff2adcc3c944a63f7cafb386abc9a1d95528966085685ae9fab2a1c0bedbf"
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource("setups")
    void shouldNotReturnUnauthorizedOrForbiddenWhenUserHasPermissions(
        final Method method,
        final RequestLine line,
        final Permission permission
    ) {
        final Response response = method.slice(permission).response(
            line,
            method.headers(TestAuthentication.ALICE),
            Content.EMPTY
        );
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

    @ParameterizedTest
    @MethodSource("setups")
    void shouldOkWhenAnonymousUserHasPermissions(
        final Method method,
        final RequestLine line,
        final Permission permission
    ) {
        final Response response = method.slice(new TestPolicy(permission, "anonymous", "Alice"))
            .response(line, Headers.EMPTY, Content.EMPTY);
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
        final Blob config = this.docker.repo(new RepoName.Valid("my-alpine")).layers()
            .put(new TrustedBlobSource(content))
            .toCompletableFuture().join();
        final byte[] data = String.format(
            "{\"config\":{\"digest\":\"%s\"},\"layers\":[],\"mediaType\":\"my-type\"}",
            config.digest().string()
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
                new RequestLine(RqMethod.PUT, "/v2/my-alpine/manifests/latest"),
                new DockerRepositoryPermission("*", "my-alpine", DockerActions.OVERWRITE.mask())
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
            ),
            Arguments.of(
                method,
                new RequestLine(RqMethod.GET, "/v2/_catalog"),
                new DockerRegistryPermission("*", RegistryCategory.ANY.mask())
            )
        );
    }

    /**
     * Authentication method.
     *
     * @since 0.8
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
            this(new AstoDocker(new InMemoryStorage()));
        }

        @Override
        public Slice slice(final Policy<PermissionCollection> policy) {
            return new DockerSlice(
                this.docker,
                policy,
                new BasicAuthScheme(new TestAuthentication()),
                Optional.empty(),
                "*"
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
                new AstoDocker(new InMemoryStorage()),
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
                Optional.empty(),
                "*"
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

    /**
     * Policy for test.
     *
     * @since 0.18
     */
    static final class TestPolicy implements Policy<PermissionCollection> {

        /**
         * Permission.
         */
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

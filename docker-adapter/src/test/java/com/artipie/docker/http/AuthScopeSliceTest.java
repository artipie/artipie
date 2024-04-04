/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.perms.DockerActions;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Response;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.rq.RequestLine;
import org.apache.commons.lang3.NotImplementedException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for {@link AuthScopeSlice}.
 */
class AuthScopeSliceTest {

    @Test
    void testScope() {
        final RequestLine line = RequestLine.from("GET /resource.txt HTTP/1.1");
        final AtomicReference<String> perm = new AtomicReference<>();
        final AtomicReference<RequestLine> aline = new AtomicReference<>();
        new AuthScopeSlice(
            new ScopeSlice() {
                @Override
                public DockerRepositoryPermission permission(RequestLine line, String registryName) {
                    aline.set(line);
                    return new DockerRepositoryPermission(registryName, "bar", DockerActions.PULL.mask());
                }

                @Override
                public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
                    return ResponseBuilder.ok().completedFuture();
                }
            },
            (headers, rline) -> CompletableFuture.completedFuture(
                AuthScheme.result(new AuthUser("alice", "test"), "")
            ),
            authUser -> new TestCollection(perm),
            "my-repo"
        ).response(line, Headers.EMPTY, Content.EMPTY).join();
        MatcherAssert.assertThat(
            "Request line passed to slice",
            aline.get(),
            new IsEqual<>(line)
        );
        MatcherAssert.assertThat(
            "Scope passed as action to permissions",
            perm.get(),
            new StringContains("DockerRepositoryPermission")
        );
    }

    /**
     * Policy implementation for this test.
     * @since 1.18
     */
    static final class TestCollection extends PermissionCollection implements java.io.Serializable {

        /**
         * Required serial.
         */
        private static final long serialVersionUID = 5843247213984092155L;

        /**
         * Reference with permission.
         */
        private final AtomicReference<String> reference;

        /**
         * Ctor.
         * @param reference Reference with permission
         */
        TestCollection(final AtomicReference<String> reference) {
            this.reference = reference;
        }

        @Override
        public void add(final Permission permission) {
            throw new NotImplementedException("Not required");
        }

        @Override
        public boolean implies(final Permission permission) {
            this.reference.set(permission.toString());
            return true;
        }

        @Override
        public Enumeration<Permission> elements() {
            return null;
        }
    }
}

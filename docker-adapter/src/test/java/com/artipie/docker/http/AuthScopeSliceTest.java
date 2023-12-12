/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.docker.perms.DockerActions;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.rs.StandardRs;
import java.nio.ByteBuffer;
import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.NotImplementedException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

/**
 * Tests for {@link AuthScopeSlice}.
 *
 * @since 0.11
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class AuthScopeSliceTest {

    @Test
    void testScope() {
        final String line = "GET /resource.txt HTTP/1.1";
        final AtomicReference<String> perm = new AtomicReference<>();
        final AtomicReference<String> aline = new AtomicReference<>();
        new AuthScopeSlice(
            new ScopeSlice() {
                @Override
                public DockerRepositoryPermission permission(final String rqline,
                    final String name) {
                    aline.set(rqline);
                    return new DockerRepositoryPermission(name, "bar", DockerActions.PULL.mask());
                }

                @Override
                public Response response(
                    final String line,
                    final Iterable<Map.Entry<String, String>> headers,
                    final Publisher<ByteBuffer> body
                ) {
                    return StandardRs.OK;
                }
            },
            (headers, rline) -> CompletableFuture.completedFuture(
                AuthScheme.result(new AuthUser("alice", "test"), "")
            ),
            authUser -> new TestCollection(perm),
            "my-repo"
        ).response(line, Headers.EMPTY, Content.EMPTY).send(
            (status, headers, body) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
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

/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.http.auth.AuthUser;
import com.artipie.security.policy.Policy;
import io.vertx.core.Handler;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import java.security.Permission;
import org.apache.http.HttpStatus;

/**
 * Handler to check that user has required permission. If permission is present,
 * vertx passes the request to the next handler (as {@link RoutingContext#next()} method is called),
 * otherwise {@link HttpStatus#SC_FORBIDDEN} is returned and request processing is finished.
 * @since 0.30
 */
public final class AuthzHandler implements Handler<RoutingContext> {

    /**
     * Artipie security policy.
     */
    private final Policy<?> policy;

    /**
     * Permission required for operation.
     */
    private final Permission perm;

    /**
     * Ctor.
     * @param policy Artipie security policy
     * @param perm Permission required for operation
     */
    public AuthzHandler(final Policy<?> policy, final Permission perm) {
        this.policy = policy;
        this.perm = perm;
    }

    @Override
    public void handle(final RoutingContext context) {
        final User usr = context.user();
        if (this.policy.getPermissions(
            new AuthUser(
                usr.principal().getString(AuthTokenRest.SUB),
                usr.principal().getString(AuthTokenRest.CONTEXT)
            )
        ).implies(this.perm)) {
            context.next();
        } else {
            context.response().setStatusCode(HttpStatus.SC_FORBIDDEN).end();
        }
    }
}

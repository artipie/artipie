/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import io.vertx.ext.web.RoutingContext;

/**
 * Repository name.
 *
 * @since 0.26
 */
public class RepositoryName {
    /**
     * Username path parameter name.
     */
    public static final String UNAME = "uname";

    /**
     * Repository path parameter name.
     */
    public static final String RNAME = "rname";

    /**
     * Layout.
     */
    private final String layout;

    /**
     * Repository name.
     */
    private final String rname;

    /**
     * User name.
     */
    private final String uname;

    /**
     * Ctor.
     *
     * @param ctx RoutingContext
     * @param layout Layout
     */
    public RepositoryName(final RoutingContext ctx, final String layout) {
        this.layout = layout;
        this.rname = ctx.pathParam(RepositoryName.RNAME);
        this.uname = ctx.pathParam(RepositoryName.UNAME);
    }

    /**
     * Provides string representation of repository name in toString() method.
     * <ul>
     *     <li>'reponame' for flat layout</li>
     *     <li>'username/reponame' for org layout</li>
     * </ul>
     * @checkstyle NoJavadocForOverriddenMethodsCheck (10 lines)
     */
    @Override
    public String toString() {
        final String reponame;
        if ("flat".equals(this.layout)) {
            reponame = this.rname;
        } else {
            reponame = String.format("%s/%s", this.uname, this.rname);
        }
        return reponame;
    }
}

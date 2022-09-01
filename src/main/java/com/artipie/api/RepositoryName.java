/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.api;

import io.vertx.ext.web.RoutingContext;

/**
 * Repository name.
 * @since 0.26
 */
public interface RepositoryName {
    /**
     * Username path parameter name.
     */
    String UNAME = "uname";

    /**
     * Repository path parameter name.
     */
    String RNAME = "rname";

    /**
     * String representation of repository name.
     * For flat layout consists of 'reponame'
     * For org layout consists of 'username/reponame'
     * @return Repository name as string
     */
    String string();

    /**
     * Decorator based on request and layout.
     * @since 0.26
     */
    class FromRequest implements RepositoryName {
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
         * @param ctx RoutingContext
         * @param layout Layout
         */
        public FromRequest(final RoutingContext ctx, final String layout) {
            this.layout = layout;
            this.rname = ctx.pathParam(RepositoryName.RNAME);
            this.uname = ctx.pathParam(RepositoryName.UNAME);
        }

        @Override
        public String string() {
            final String reponame;
            if ("flat".equals(this.layout)) {
                reponame = this.rname;
            } else {
                reponame = String.format("%s/%s", this.uname, this.rname);
            }
            return reponame;
        }
    }
}

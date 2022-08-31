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
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    class FromRequest implements RepositoryName {
        /**
         * Decorated repository name.
         */
        private final RepositoryName origin;

        /**
         * Ctor.
         * @param ctx RoutingContext
         * @param layout Layout
         */
        public FromRequest(final RoutingContext ctx, final String layout) {
            if ("flat".equals(layout)) {
                this.origin = new RepositoryName.FlatRepositoryName(
                    ctx.pathParam(RepositoryName.RNAME)
                );
            } else {
                this.origin = new RepositoryName.OrgRepositoryName(
                    ctx.pathParam(RepositoryName.UNAME),
                    ctx.pathParam(RepositoryName.RNAME)
                );
            }
        }

        @Override
        public String string() {
            return this.origin.string();
        }
    }

    /**
     * Repository name for 'flat' layout.
     * @since 0.26
     */
    class FlatRepositoryName implements RepositoryName {
        /**
         * Repository name.
         */
        private final String rname;

        /**
         * Ctor.
         * @param rname Repository name
         */
        public FlatRepositoryName(final String rname) {
            this.rname = rname;
        }

        @Override
        public String string() {
            return this.rname;
        }
    }

    /**
     * Repository name for 'org' layout.
     * @since 0.26
     */
    class OrgRepositoryName implements RepositoryName {
        /**
         * User name.
         */
        private final String uname;

        /**
         * Repository name.
         */
        private final String rname;

        /**
         * Ctor.
         * @param uname User name
         * @param rname Repository name
         */
        public OrgRepositoryName(final String uname, final String rname) {
            this.uname = uname;
            this.rname = rname;
        }

        @Override
        public String string() {
            return String.format("%s/%s", this.uname, this.rname);
        }
    }
}

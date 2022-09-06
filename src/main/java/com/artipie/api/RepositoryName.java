/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.settings.Layout;
import io.vertx.ext.web.RoutingContext;

/**
 * Repository name.
 *
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
     * The name of the repository.
     * @return String name
     */
    String toString();

    /**
     * Repository name from request (from vertx {@link RoutingContext}).
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
        private final RoutingContext context;

        /**
         * Ctor.
         *
         * @param context Context
         * @param layout Layout
         */
        public FromRequest(final RoutingContext context, final String layout) {
            this.context = context;
            this.layout = layout;
        }

        /**
         * Provides string representation of repository name in toString() method.
         * <ul>
         *     <li>'reponame' for flat layout</li>
         *     <li>'username/reponame' for org layout</li>
         * </ul>
         *
         * @checkstyle NoJavadocForOverriddenMethodsCheck (10 lines)
         */
        @Override
        public String toString() {
            final String reponame;
            if (new Layout.Flat().toString().equals(this.layout)) {
                reponame = this.context.pathParam(RepositoryName.RNAME);
            } else {
                reponame = new Org(
                    this.context.pathParam(RepositoryName.RNAME),
                    this.context.pathParam(RepositoryName.UNAME)
                ).toString();
            }
            return reponame;
        }
    }

    /**
     * Repository name for flat layout.
     * @since 0.26
     */
    class Flat implements RepositoryName {

        /**
         * Repository name.
         */
        private final String name;

        /**
         * Ctor.
         * @param name Name
         */
        public Flat(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    /**
     * Repository name for org layout is combined from username and reponame:
     * 'username/reponame'.
     * @since 0.26
     */
    class Org implements RepositoryName {

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
         * @param rname Repository name
         * @param uname User name
         */
        public Org(final String rname, final String uname) {
            this.rname = rname;
            this.uname = uname;
        }

        @Override
        public String toString() {
            return String.format("%s/%s", this.uname, this.rname);
        }
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.api;

import io.vertx.ext.web.RoutingContext;

/**
 * Repository name.
 *
 * @since 0.26
 */
public interface RepositoryName {

    /**
     * Repository path parameter name.
     */
    String REPOSITORY_NAME = "rname";

    /**
     * The name of the repository.
     * @return String name
     */
    String toString();

    /**
     * Repository name from request (from vertx {@link RoutingContext}) by `rname` path parameter.
     * @since 0.26
     */
    class FromRequest implements RepositoryName {

        /**
         * Repository name.
         */
        private final RoutingContext context;

        /**
         * Ctor.
         *
         * @param context Context
         */
        public FromRequest(final RoutingContext context) {
            this.context = context;
        }

        @Override
        public String toString() {
            return this.context.pathParam(RepositoryName.REPOSITORY_NAME);
        }
    }

    /**
     * Repository name from string.
     * @since 0.26
     */
    class Simple implements RepositoryName {

        /**
         * Repository name.
         */
        private final String name;

        /**
         * Ctor.
         * @param name Name
         */
        public Simple(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

}

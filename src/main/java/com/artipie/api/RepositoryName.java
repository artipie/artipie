/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.api;

/**
 * Repository name.
 * @since 0.26
 */
public interface RepositoryName {
    /**
     * String representation of repository name.
     * For flat layout consists of 'reponame'
     * For org layout consists of 'username/reponame'
     * @return Repository name as string
     */
    String string();

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

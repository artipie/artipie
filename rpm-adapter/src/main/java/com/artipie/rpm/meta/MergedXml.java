/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.rpm.pkg.InvalidPackageException;
import com.artipie.rpm.pkg.Package;
import java.io.IOException;
import java.util.Collection;

/**
 * Merged xml: merge provided packages into existing xml index.
 * @since 1.5
 */
public interface MergedXml {

    /**
     * Appends provided packages to the index xml.
     * @param packages Packages to append info about
     * @param event Event constant and to append
     * @return Merge result
     * @throws IOException On error
     */
    Result merge(Collection<Package.Meta> packages, XmlEvent event) throws IOException;

    /**
     * Merge result.
     * @since 1.5
     */
    final class Result {

        /**
         * Items count.
         */
        private final long cnt;

        /**
         * Ids of the items to remove.
         */
        private final Collection<String> ids;

        /**
         * Ctor.
         * @param cnt Items count
         * @param ids Ids of the items to remove
         */
        public Result(final long cnt, final Collection<String> ids) {
            this.cnt = cnt;
            this.ids = ids;
        }

        /**
         * Get packages count.
         * @return Count
         */
        public long count() {
            return this.cnt;
        }

        /**
         * Get packages checksums (ids).
         * @return Checksums
         */
        public Collection<String> checksums() {
            return this.ids;
        }
    }

    /**
     * Handles invalid rpm packages.
     * @since 1.7
     */
    final class InvalidPackage {

        /**
         * Action to perform.
         */
        private final Action action;

        /**
         * Should invalid package be skipped?
         */
        private final boolean skip;

        /**
         * Ctor.
         * @param action Action to perform
         * @param skip Should invalid package be skipped?
         */
        public InvalidPackage(final Action action, final boolean skip) {
            this.action = action;
            this.skip = skip;
        }

        /**
         * Handles {@link InvalidPackageException}.
         * @throws IOException On error
         */
        void handle() throws IOException {
            try {
                this.action.perform();
            } catch (final InvalidPackageException err) {
                if (!this.skip) {
                    throw err;
                }
            }
        }
    }

    /**
     * Action.
     * @since 1.7
     */
    @FunctionalInterface
    interface Action {

        /**
         * Perform action.
         * @throws IOException On error
         */
        void perform() throws IOException;
    }
}

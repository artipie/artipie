/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.asto.Key;
import java.util.concurrent.CompletableFuture;

/**
 * The NPM publish front. Publish new packages in different ways
 * (e.g. using `npm publish` or using `curl PUT`).
 * @since 0.9
 */
public interface Publish {
    /**
     * Publish a new version of a npm package and returned published package info.
     *
     * @param prefix Path prefix for archives and meta information storage
     * @param artifact Where uploaded json file is stored
     * @return Completion with new package information or error signal.
     */
    CompletableFuture<PackageInfo> publishWithInfo(Key prefix, Key artifact);

    /**
     * Publish a new version of a npm package.
     *
     * @param prefix Path prefix for archives and meta information storage
     * @param artifact Where uploaded json file is stored
     * @return Completion with new package information or error signal.
     */
    CompletableFuture<Void> publish(Key prefix, Key artifact);

    /**
     * Package info.
     * @since 0.12
     */
    class PackageInfo {

        /**
         * Package name.
         */
        private final String name;

        /**
         * Version.
         */
        private final String version;

        /**
         * Package tar archive size.
         */
        private final long size;

        /**
         * Ctor.
         * @param name Package name
         * @param version Version
         * @param size Package tar archive size
         */
        public PackageInfo(final String name, final String version, final long size) {
            this.name = name;
            this.version = version;
            this.size = size;
        }

        /**
         * Package name (unique id).
         * @return String name
         */
        public String packageName() {
            return this.name;
        }

        /**
         * Package version.
         * @return String SemVer compatible version
         */
        public String packageVersion() {
            return this.version;
        }

        /**
         * Package tar archive size.
         * @return Long size
         */
        public long tarSize() {
            return this.size;
        }
    }
}

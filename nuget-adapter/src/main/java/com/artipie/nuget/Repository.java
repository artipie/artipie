/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.nuget;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.nuget.metadata.Nuspec;
import com.artipie.nuget.metadata.NuspecField;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * NuGet repository.
 *
 * @since 0.5
 */
public interface Repository {

    /**
     * Read package content.
     *
     * @param key Package content key.
     * @return Content if exists, empty otherwise.
     */
    CompletionStage<Optional<Content>> content(Key key);

    /**
     * Adds NuGet package in .nupkg file format from storage.
     *
     * @param content Content of .nupkg package.
     * @return Completion of adding package.
     */
    CompletionStage<PackageInfo> add(Content content);

    /**
     * Enumerates package versions.
     *
     * @param id Package identifier.
     * @return Versions of package.
     */
    CompletionStage<Versions> versions(PackageKeys id);

    /**
     * Read package description in .nuspec format.
     *
     * @param identity Package identity consisting of package id and version.
     * @return Package description in .nuspec format.
     */
    CompletionStage<Nuspec> nuspec(PackageIdentity identity);

    /**
     * Package info.
     * @since 1.6
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
        public PackageInfo(final NuspecField name, final NuspecField version, final long size) {
            this.name = name.normalized();
            this.version = version.normalized();
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
         * Package zip archive (nupkg) size.
         * @return Long size
         */
        public long zipSize() {
            return this.size;
        }
    }
}

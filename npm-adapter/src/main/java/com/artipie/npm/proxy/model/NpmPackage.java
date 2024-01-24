/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.proxy.model;

import io.vertx.core.json.JsonObject;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * NPM Package.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public final class NpmPackage {
    /**
     * Package name.
     */
    private final String name;

    /**
     * JSON data.
     */
    private final String content;

    /**
     * Package metadata.
     */
    private final Metadata metadata;

    /**
     * Ctor.
     * @param name Package name
     * @param content JSON data
     * @param modified Last modified date
     * @param refreshed Last update date
     */
    public NpmPackage(final String name,
        final String content,
        final String modified,
        final OffsetDateTime refreshed) {
        this(name, content, new Metadata(modified, refreshed));
    }

    /**
     * Ctor.
     * @param name Package name
     * @param content JSON data
     * @param metadata Package metadata
     */
    public NpmPackage(final String name, final String content, final Metadata metadata) {
        this.name = name;
        this.content = content;
        this.metadata = metadata;
    }

    /**
     * Get package name.
     * @return Package name
     */
    public String name() {
        return this.name;
    }

    /**
     * Get package JSON.
     * @return Package JSON
     */
    public String content() {
        return this.content;
    }

    /**
     * Get package metadata.
     * @return Package metadata
     */
    public Metadata meta() {
        return this.metadata;
    }

    /**
     * NPM Package metadata.
     * @since 0.2
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public static class Metadata {
        /**
         * Last modified date.
         */
        private final String modified;

        /**
         * Last refreshed date.
         */
        private final OffsetDateTime refreshed;

        /**
         * Ctor.
         * @param json JSON representation of metadata
         */
        public Metadata(final JsonObject json) {
            this(
                json.getString("last-modified"),
                OffsetDateTime.parse(
                    json.getString("last-refreshed"),
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
                )
            );
        }

        /**
         * Ctor.
         * @param modified Last modified date
         * @param refreshed Last refreshed date
         */
        Metadata(final String modified, final OffsetDateTime refreshed) {
            this.modified = modified;
            this.refreshed = refreshed;
        }

        /**
         * Get last modified date.
         * @return Last modified date
         */
        public String lastModified() {
            return this.modified;
        }

        /**
         * Get last refreshed date.
         * @return The date of last attempt to refresh metadata
         */
        public OffsetDateTime lastRefreshed() {
            return this.refreshed;
        }

        /**
         * Get JSON representation of metadata.
         * @return JSON representation
         */
        public JsonObject json() {
            final JsonObject json = new JsonObject();
            json.put("last-modified", this.modified);
            json.put(
                "last-refreshed",
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(this.refreshed)
            );
            return json;
        }
    }
}

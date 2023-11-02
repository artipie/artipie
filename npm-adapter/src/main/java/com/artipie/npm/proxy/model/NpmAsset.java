/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.npm.proxy.model;

import io.vertx.core.json.JsonObject;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Publisher;

/**
 * NPM Asset.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public final class NpmAsset {
    /**
     * Asset path.
     */
    private final String path;

    /**
     * Reactive publisher for asset content.
     */
    private final Publisher<ByteBuffer> content;

    /**
     * Asset metadata.
     */
    private final Metadata metadata;

    /**
     * Ctor.
     * @param path Asset path
     * @param content Reactive publisher for asset content
     * @param modified Last modified date
     * @param ctype Original content type
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    public NpmAsset(final String path,
        final Publisher<ByteBuffer> content,
        final String modified,
        final String ctype) {
        this(path, content, new Metadata(modified, ctype));
    }

    /**
     * Ctor.
     * @param path Asset path
     * @param content Reactive publisher for asset content
     * @param metadata Asset metadata
     */
    public NpmAsset(final String path,
        final Publisher<ByteBuffer> content,
        final Metadata metadata) {
        this.path = path;
        this.content = content;
        this.metadata = metadata;
    }

    /**
     * Return asset path.
     * @return Asset path
     */
    public String path() {
        return this.path;
    }

    /**
     * Get reactive data publisher.
     * @return Data publisher
     */
    public Publisher<ByteBuffer> dataPublisher() {
        return this.content;
    }

    /**
     * Get asset metadata.
     * @return Asset metadata
     */
    public Metadata meta() {
        return this.metadata;
    }

    /**
     * NPM asset metadata.
     * @since 0.2
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public static class Metadata {
        /**
         * Last modified date.
         */
        private final String modified;

        /**
         * Original content type.
         */
        private final String ctype;

        /**
         * Ctor.
         * @param json JSON representation of metadata
         */
        public Metadata(final JsonObject json) {
            this(
                json.getString("last-modified"),
                Optional.ofNullable(json.getString("content-type"))
                    .orElse("application/octet-stream")
            );
        }

        /**
         * Ctor.
         * @param modified Last modified date
         * @param ctype Content type
         */
        Metadata(final String modified, final String ctype) {
            this.modified = modified;
            this.ctype = ctype;
        }

        /**
         * Get last modified date.
         * @return Last modified date
         */
        public String lastModified() {
            return this.modified;
        }

        /**
         * Get original content type.
         * @return Original content type
         */
        public String contentType() {
            return this.ctype;
        }

        /**
         * Get JSON representation of metadata.
         * @return JSON representation
         */
        public JsonObject json() {
            final JsonObject json = new JsonObject();
            json.put("last-modified", this.modified);
            json.put("content-type", this.ctype);
            return json;
        }
    }
}

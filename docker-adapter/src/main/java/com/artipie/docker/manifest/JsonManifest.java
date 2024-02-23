/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.manifest;

import com.artipie.asto.Content;
import com.artipie.docker.Digest;
import com.artipie.docker.error.InvalidManifestException;
import com.google.common.base.Strings;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * Image manifest in JSON format.
 *
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class JsonManifest implements Manifest {

    private static JsonObject readJson(byte[] data) {
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(data))) {
            return reader.readObject();
        }
    }
    /**
     * Manifest digest.
     */
    private final Digest manifestDigest;

    /**
     * JSON bytes.
     */
    private final byte[] source;

    private final JsonObject json;

    /**
     * Ctor.
     *
     * @param manifestDigest Manifest digest.
     * @param source JSON bytes.
     */
    public JsonManifest(final Digest manifestDigest, final byte[] source) {
        this.manifestDigest = manifestDigest;
        this.source = Arrays.copyOf(source, source.length);
        this.json = readJson(this.source);
    }

    @Override
    public String mediaType() {
        String res = this.json.getString("mediaType", null);
        if (Strings.isNullOrEmpty(res)) {
            throw new InvalidManifestException("Required field `mediaType` is absent");
        }
        return res;
    }

    @Override
    public Digest config() {
        JsonObject config = this.json.getJsonObject("config");
        if (config == null) {
            throw new InvalidManifestException("Required field `config` is absent");
        }
        return new Digest.FromString(config.getString("digest"));
    }

    @Override
    public Collection<Layer> layers() {
        JsonArray array = this.json.getJsonArray("layers");
        if (array == null) {
            throw new InvalidManifestException("Required field `layers` is absent");
        }
        return array.getValuesAs(JsonValue::asJsonObject)
                .stream()
                .map(JsonLayer::new)
                .collect(Collectors.toList());
    }

    @Override
    public Digest digest() {
        return this.manifestDigest;
    }

    @Override
    public Content content() {
        return new Content.From(this.source);
    }

    @Override
    public long size() {
        return this.source.length;
    }

    /**
     * Image layer description in JSON format.
     *
     * @since 0.2
     */
    private static final class JsonLayer implements Layer {

        /**
         * JSON object.
         */
        private final JsonObject json;

        /**
         * Ctor.
         *
         * @param json JSON object.
         */
        private JsonLayer(final JsonObject json) {
            this.json = json;
        }

        @Override
        public Digest digest() {
            return new Digest.FromString(this.json.getString("digest"));
        }

        @Override
        public Collection<URL> urls() {
            JsonArray urls = this.json.getJsonArray("urls");
            if (urls == null) {
                return Collections.emptyList();
            }
            return urls.getValuesAs(JsonString.class)
                    .stream()
                    .map(
                            str -> {
                                try {
                                    return URI.create(str.getString()).toURL();
                                } catch (final MalformedURLException ex) {
                                    throw new IllegalArgumentException(ex);
                                }
                            }
                    )
                    .collect(Collectors.toList());
        }

        @Override
        public long size() {
            JsonNumber res = this.json.getJsonNumber("size");
            return res != null ? res.longValue() : 0L;
        }
    }
}

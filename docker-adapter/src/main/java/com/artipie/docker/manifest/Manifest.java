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
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Image manifest in JSON format.
 */
public final class Manifest {

    /**
     * New image manifest format (schemaVersion = 2).
     */
    public static final String MANIFEST_SCHEMA2 = "application/vnd.docker.distribution.manifest.v2+json";

    /**
     * Image Manifest OCI Specification.
     */
    public static final String MANIFEST_OCI_V1 = "application/vnd.oci.image.manifest.v1+json";

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
     * @param manifestDigest Manifest digest.
     * @param source JSON bytes.
     */
    public Manifest(final Digest manifestDigest, final byte[] source) {
        this.manifestDigest = manifestDigest;
        this.source = Arrays.copyOf(source, source.length);
        this.json = readJson(this.source);
    }

    private static JsonObject readJson(final byte[] data) {
        try (JsonReader reader = Json.createReader(new ByteArrayInputStream(data))) {
            return reader.readObject();
        } catch (JsonException e){
            throw new InvalidManifestException("JSON reading error", e);
        }
    }

    /**
     * The MIME type of the manifest.
     *
     * @return The MIME type.
     */
    public String mediaType() {
        String res = this.json.getString("mediaType", null);
        if (Strings.isNullOrEmpty(res)) {
            throw new InvalidManifestException("Required field `mediaType` is absent");
        }
        return res;
    }

    /**
     * Read config digest.
     *
     * @return Config digests.
     */
    public Digest config() {
        JsonObject config = this.json.getJsonObject("config");
        if (config == null) {
            throw new InvalidManifestException("Required field `config` is absent");
        }
        return new Digest.FromString(config.getString("digest"));
    }

    /**
     * Read layer digests.
     *
     * @return Layer digests.
     */
    public Collection<ManifestLayer> layers() {
        JsonArray array = this.json.getJsonArray("layers");
        if (array == null) {
            throw new InvalidManifestException("Required field `layers` is absent");
        }
        return array.getValuesAs(JsonValue::asJsonObject)
                .stream()
                .map(ManifestLayer::new)
                .collect(Collectors.toList());
    }

    /**
     * Manifest digest.
     *
     * @return Digest.
     */
    public Digest digest() {
        return this.manifestDigest;
    }

    /**
     * Read manifest binary content.
     *
     * @return Manifest binary content.
     */
    public Content content() {
        return new Content.From(this.source);
    }

    /**
     * Manifest size.
     *
     * @return Size of the manifest.
     */
    public long size() {
        return this.source.length;
    }
}

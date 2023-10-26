/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import io.reactivex.Flowable;
import java.io.StringReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;

/**
 * Prepends all tarball references in the package metadata json with the prefix to build
 * absolute URL: /@scope/package-name -&gt; http://host:port/base-path/@scope/package-name.
 * @since 0.6
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class Tarballs {

    /**
     * Original content.
     */
    private final Content original;

    /**
     * URL prefix.
     */
    private final URL prefix;

    /**
     * Ctor.
     * @param original Original content
     * @param prefix URL prefix
     */
    public Tarballs(final Content original, final URL prefix) {
        this.original = original;
        this.prefix = prefix;
    }

    /**
     * Return modified content with prepended URLs.
     * @return Modified content with prepended URLs
     */
    public Content value() {
        return new Content.From(
            new Concatenation(this.original)
                .single()
                .map(ByteBuffer::array)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .map(json -> Json.createReader(new StringReader(json)).readObject())
                .map(json -> Tarballs.updateJson(json, this.prefix.toString()))
                .flatMapPublisher(
                    json -> new Content.From(
                        Flowable.fromArray(
                            ByteBuffer.wrap(
                                json.toString().getBytes(StandardCharsets.UTF_8)
                            )
                        )
                    )
                )
        );
    }

    /**
     * Replaces tarball links with absolute paths based on prefix.
     * @param original Original JSON object
     * @param prefix Links prefix
     * @return Transformed JSON object
     */
    private static JsonObject updateJson(final JsonObject original, final String prefix) {
        final JsonPatchBuilder builder = Json.createPatchBuilder();
        final Set<String> versions = original.getJsonObject("versions").keySet();
        for (final String version : versions) {
            builder.add(
                String.format("/versions/%s/dist/tarball", version),
                String.join(
                    "",
                    prefix.replaceAll("/$", ""),
                    original.getJsonObject("versions").getJsonObject(version)
                        .getJsonObject("dist").getString("tarball")
                )
            );
        }
        return builder.build().apply(original);
    }
}

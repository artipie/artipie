/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.composer;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.composer.misc.ContentAsJson;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * PHP Composer packages registry built from JSON.
 *
 * @since 0.1
 */
public final class JsonPackages implements Packages {

    /**
     * Root attribute value for packages registry in JSON.
     */
    private static final String ATTRIBUTE = "packages";

    /**
     * Packages registry content.
     */
    private final Content source;

    /**
     * Ctor.
     */
    public JsonPackages() {
        this(
            toContent(
                Json.createObjectBuilder()
                    .add(JsonPackages.ATTRIBUTE, Json.createObjectBuilder())
                    .build()
            )
        );
    }

    /**
     * Ctor.
     *
     * @param source Packages registry content.
     */
    public JsonPackages(final Content source) {
        this.source = source;
    }

    @Override
    public CompletionStage<Packages> add(final Package pack, final Optional<String> vers) {
        return new ContentAsJson(this.source)
            .value()
            .thenCompose(
                json -> {
                    if (json.isNull(JsonPackages.ATTRIBUTE)) {
                        throw new IllegalStateException("Bad content, no 'packages' object found");
                    }
                    final JsonObject pkgs = json.getJsonObject(JsonPackages.ATTRIBUTE);
                    return pack.name()
                        .thenApply(Name::string)
                        .thenCompose(
                            pname -> {
                                final JsonObjectBuilder builder;
                                if (pkgs.isEmpty() || pkgs.isNull(pname)) {
                                    builder = Json.createObjectBuilder();
                                } else {
                                    builder = Json.createObjectBuilder(pkgs.getJsonObject(pname));
                                }
                                return pack.version(vers).thenCombine(
                                    pack.json(),
                                    (vrsn, pkg) -> {
                                        if (!vrsn.isPresent()) {
                                            // @checkstyle LineLengthCheck (1 line)
                                            throw new IllegalStateException(String.format("Failed to add package `%s` to packages.json because version is absent", pname));
                                        }
                                        final JsonObject foradd;
                                        if (pkg.containsKey(JsonPackage.VRSN)) {
                                            foradd = pkg;
                                        } else {
                                            foradd = Json.createObjectBuilder(pkg)
                                                .add(JsonPackage.VRSN, vrsn.get())
                                                .build();
                                        }
                                        return builder.add(vrsn.get(), foradd);
                                    }
                                ).thenApply(
                                    bldr -> new JsonPackages(
                                        toContent(
                                            Json.createObjectBuilder(json)
                                                .add(
                                                    JsonPackages.ATTRIBUTE,
                                                    Json.createObjectBuilder(pkgs).add(pname, bldr)
                                                ).build()
                                        )
                                    )
                                );
                            }
                        );
                }
            );
    }

    @Override
    public CompletionStage<Void> save(final Storage storage, final Key key) {
        return this.content()
            .thenCompose(content -> storage.save(key, content));
    }

    @Override
    public CompletionStage<Content> content() {
        return CompletableFuture.completedFuture(this.source);
    }

    /**
     * Serializes JSON object into content.
     *
     * @param json JSON object.
     * @return Serialized JSON object.
     */
    private static Content toContent(final JsonObject json) {
        return new Content.From(json.toString().getBytes(StandardCharsets.UTF_8));
    }
}

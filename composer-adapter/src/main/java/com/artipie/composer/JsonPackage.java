/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.composer;

import com.artipie.asto.Content;
import com.artipie.composer.misc.ContentAsJson;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.json.JsonObject;

/**
 * PHP Composer package built from JSON.
 *
 * @since 0.1
 */
public final class JsonPackage implements Package {
    /**
     * Key for version in JSON.
     */
    public static final String VRSN = "version";

    /**
     * Package binary content.
     */
    private final Content content;

    /**
     * Ctor.
     *
     * @param content Package binary content.
     */
    public JsonPackage(final Content content) {
        this.content = content;
    }

    @Override
    public CompletionStage<Name> name() {
        return this.mandatoryString("name")
            .thenApply(Name::new);
    }

    @Override
    public CompletionStage<Optional<String>> version(final Optional<String> value) {
        final String version = value.orElse(null);
        return this.optString(JsonPackage.VRSN)
            .thenApply(opt -> opt.orElse(version))
            .thenApply(Optional::ofNullable);
    }

    @Override
    public CompletionStage<JsonObject> json() {
        return new ContentAsJson(this.content).value();
    }

    /**
     * Reads string value from package JSON root. Throws exception if value not found.
     *
     * @param name Attribute value.
     * @return String value.
     */
    private CompletionStage<String> mandatoryString(final String name) {
        return this.json()
            .thenApply(jsn -> jsn.getString(name))
            .thenCompose(
                val -> {
                    final CompletionStage<String> res;
                    if (val == null) {
                        res = new CompletableFuture<String>()
                            .exceptionally(
                                ignore -> {
                                    throw new IllegalStateException(
                                        String.format("Bad package, no '%s' found.", name)
                                    );
                                }
                        );
                    } else {
                        res = CompletableFuture.completedFuture(val);
                    }
                    return res;
                }
            );
    }

    /**
     * Reads string value from package JSON root. Empty in case of absence.
     * @param name Attribute value
     * @return String value, otherwise empty.
     */
    private CompletionStage<Optional<String>> optString(final String name) {
        return this.json()
            .thenApply(json -> json.getString(name, null))
            .thenApply(Optional::ofNullable);
    }
}

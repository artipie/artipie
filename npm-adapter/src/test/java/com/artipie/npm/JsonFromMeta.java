/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;

/**
 * Json object from meta file for usage in tests.
 * @since 0.9
 */
public final class JsonFromMeta {
    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Path to `meta.json` file.
     */
    private final Key path;

    /**
     * Ctor.
     * @param storage Storage
     * @param path Path to `meta.json` file
     */
    public JsonFromMeta(final Storage storage, final Key path) {
        this.storage = storage;
        this.path = path;
    }

    /**
     * Obtains json from meta file.
     * @return Json from meta file.
     */
    public JsonObject json() {
        return Json.createReader(
            new StringReader(
                new PublisherAs(
                    this.storage.value(new Key.From(this.path, "meta.json")).join()
                ).asciiString()
                    .toCompletableFuture().join()
            )
        ).readObject();
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm.test;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.helm.metadata.IndexYaml;
import com.artipie.helm.metadata.IndexYamlMapping;

/**
 * Class for using test scope. It helps to get content of index from storage.
 * @since 0.3
 */
public final class ContentOfIndex {
    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Storage
     */
    public ContentOfIndex(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Obtains index from storage by default key.
     * @return Index file from storage.
     */
    public IndexYamlMapping index() {
        return this.index(IndexYaml.INDEX_YAML);
    }

    /**
     * Obtains index from storage by specified path.
     * @param path Path to index file
     * @return Index file from storage.
     */
    public IndexYamlMapping index(final Key path) {
        return new IndexYamlMapping(
            new PublisherAs(
                this.storage.value(path).join()
            ).asciiString()
            .toCompletableFuture().join()
        );
    }
}

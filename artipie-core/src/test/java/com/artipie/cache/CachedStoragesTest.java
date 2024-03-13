/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.cache;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.ArtipieException;
import com.artipie.asto.Storage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StoragesCache}.
 */
final class CachedStoragesTest {

    @Test
    void getsValueFromCache() {
        final String path = "same/path/for/storage";
        final StoragesCache cache = new StoragesCache();
        final Storage storage = cache.storage(this.config(path));
        final Storage same = cache.storage(this.config(path));
        Assertions.assertEquals(storage, same);
        Assertions.assertEquals(1L, cache.size());
    }

    @Test
    void getsOriginForDifferentConfiguration() {
        final StoragesCache cache = new StoragesCache();
        final Storage first = cache.storage(this.config("first"));
        final Storage second = cache.storage(this.config("second"));
        Assertions.assertNotEquals(first, second);
        Assertions.assertEquals(2L, cache.size());
    }

    @Test
    void failsToGetStorageWhenSectionIsAbsent() {
        Assertions.assertThrows(
            ArtipieException.class,
            () -> new StoragesCache().storage(
                Yaml.createYamlMappingBuilder().build()
            )
        );
    }

    private YamlMapping config(final String path) {
        return Yaml.createYamlMappingBuilder()
            .add("type", "fs")
            .add("path", path).build();
    }
}

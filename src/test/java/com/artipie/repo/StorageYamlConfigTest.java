/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.repo;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.StorageAliases;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link StorageYamlConfig}.
 * @since 0.14
 */
final class StorageYamlConfigTest {

    /**
     * Type storage.
     */
    private static final String TYPE = "type";

    /**
     * Path storage.
     */
    private static final String PATH = "path";

    @Test
    void throwsExceptionForInvalidStorageConfig() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new StorageYamlConfig(
                Yaml.createYamlSequenceBuilder()
                    .add("wrong because sequence").build(),
                StorageAliases.EMPTY
            ).storage()
        );
    }

    @Test
    void worksForValidYamlConfig() {
        MatcherAssert.assertThat(
            this.config().storage(),
            new IsInstanceOf(Storage.class)
        );
    }

    @Test
    void worksForYamlConfigWithAlias() {
        final String alias = "default";
        MatcherAssert.assertThat(
            new StorageYamlConfig(
                Yaml.createYamlScalarBuilder().addLine(alias).buildPlainScalar(),
                new StorageAliases.FromYaml(
                    Yaml.createYamlMappingBuilder().add(
                        "storages", Yaml.createYamlMappingBuilder()
                            .add(
                                alias, Yaml.createYamlMappingBuilder()
                                    .add(StorageYamlConfigTest.TYPE, "fs")
                                    .add(StorageYamlConfigTest.PATH, "/some/path")
                                    .build()
                            ).build()
                    ).build()
                )
            ).storage(),
            new IsInstanceOf(Storage.class)
        );
    }

    @Test
    void returnsSubStorage() {
        MatcherAssert.assertThat(
            this.config().subStorage(new Key.From()),
            new IsInstanceOf(Storage.class)
        );
    }

    private StorageYamlConfig config() {
        return new StorageYamlConfig(
            Yaml.createYamlMappingBuilder()
                .add(StorageYamlConfigTest.TYPE, "fs")
                .add(StorageYamlConfigTest.PATH, "some path")
                .build(),
            StorageAliases.EMPTY
        );
    }

}

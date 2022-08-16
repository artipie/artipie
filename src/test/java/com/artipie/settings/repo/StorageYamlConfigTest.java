/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.fs.FileStorage;
import com.artipie.settings.StorageAliases;
import java.io.IOException;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link StorageYamlConfig}.
 * @since 0.14
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class StorageYamlConfigTest {

    /**
     * Temporary directory for file storage.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

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
    void worksForValidYamlConfig() throws IOException {
        MatcherAssert.assertThat(
            this.config().storage(),
            new IsInstanceOf(Storage.class)
        );
    }

    @Test
    void worksForYamlConfigWithAlias() throws IOException {
        final String alias = "default";
        MatcherAssert.assertThat(
            new StorageYamlConfig(
                Yaml.createYamlScalarBuilder().addLine(alias).buildPlainScalar(),
                new StorageAliases.FromYaml(
                    Yaml.createYamlMappingBuilder().add(
                        "storages", Yaml.createYamlMappingBuilder()
                            .add(
                                alias, this.storageYaml()
                            ).build()
                    ).build()
                )
            ).storage(),
            new IsInstanceOf(Storage.class)
        );
    }

    @Test
    void returnsSubStorage() throws IOException {
        MatcherAssert.assertThat(
            this.config().subStorage(new Key.From()),
            new IsInstanceOf(SubStorage.class)
        );
    }

    @Test
    void returnsSubStorageWithCorrectPrefix() throws IOException {
        final Key prefix = new Key.From("prefix");
        final Key path = new Key.From("some/path");
        final Key full = new Key.From(prefix, path);
        final byte[] data = "content".getBytes();
        this.config().subStorage(prefix)
            .save(path, new Content.From(data))
            .join();
        MatcherAssert.assertThat(
            new PublisherAs(
                new FileStorage(this.tmp)
                    .value(full)
                    .join()
            ).bytes()
            .toCompletableFuture().join(),
            new IsEqual<>(data)
        );
    }

    private StorageYamlConfig config() throws IOException {
        return new StorageYamlConfig(
            this.storageYaml(),
            StorageAliases.EMPTY
        );
    }

    private YamlMapping storageYaml() throws IOException {
        return Yaml.createYamlMappingBuilder()
            .add("type", "fs")
            .add("path", this.tmp.toString())
            .build();
    }

}

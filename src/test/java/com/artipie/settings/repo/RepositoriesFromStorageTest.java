/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ValueNotFoundException;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.settings.RepositoriesFromStorage;
import com.artipie.settings.StorageAliases;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link RepositoriesFromStorage}.
 *
 * @since 0.14
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class RepositoriesFromStorageTest {

    /**
     * Repo name.
     */
    private static final String REPO = "my-repo";

    /**
     * Type repository.
     */
    private static final String TYPE = "maven";

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @ParameterizedTest
    @CsvSource({"_storages.yaml", "_storages.yml"})
    void findRepoSettingAndCreateRepoConfigWithStorageAlias(final String filename) {
        final String alias = "default";
        new RepoConfigYaml(RepositoriesFromStorageTest.TYPE)
            .withStorageAlias(alias)
            .saveTo(this.storage, RepositoriesFromStorageTest.REPO);
        this.saveAliasConfig(alias, filename);
        MatcherAssert.assertThat(
            this.repoConfig()
                .storageOpt()
                .isPresent(),
            new IsEqual<>(true)
        );
    }

    @Test
    void findRepoSettingAndCreateRepoConfigWithCustomStorage() {
        new RepoConfigYaml(RepositoriesFromStorageTest.TYPE)
            .withFileStorage(Path.of("some", "somepath"))
            .saveTo(this.storage, RepositoriesFromStorageTest.REPO);
        MatcherAssert.assertThat(
            this.repoConfig()
                .storageOpt()
                .isPresent(),
            new IsEqual<>(true)
        );
    }

    @Test
    void throwsExceptionWhenConfigYamlAbsent() {
        final CompletionException result = Assertions.assertThrows(
            CompletionException.class,
            this::repoConfig
        );
        MatcherAssert.assertThat(
            result.getCause().getCause(),
            new IsInstanceOf(ValueNotFoundException.class)
        );
    }

    @Test
    void throwsExceptionWhenConfigYamlMalformedSinceWithoutStorage() {
        new RepoConfigYaml(RepositoriesFromStorageTest.TYPE)
            .saveTo(this.storage, RepositoriesFromStorageTest.REPO);
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.repoConfig()
                .storage()
        );
    }

    @Test
    void throwsExceptionWhenAliasesConfigAbsent() {
        new RepoConfigYaml(RepositoriesFromStorageTest.TYPE)
            .withStorageAlias("alias")
            .saveTo(this.storage, RepositoriesFromStorageTest.REPO);
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.repoConfig()
                .storageOpt()
        );
    }

    @Test
    void throwsExceptionWhenAliasConfigMalformedSinceSequenceInsteadMapping() {
        final String alias = "default";
        new RepoConfigYaml(RepositoriesFromStorageTest.TYPE)
            .withStorageAlias(alias)
            .saveTo(this.storage, RepositoriesFromStorageTest.REPO);
        this.storage.save(
            new Key.From(StorageAliases.FILE_NAME),
            new Content.From(
                Yaml.createYamlMappingBuilder().add(
                    "storages", Yaml.createYamlSequenceBuilder()
                        .add(
                            Yaml.createYamlMappingBuilder().add(
                                alias, Yaml.createYamlMappingBuilder()
                                    .add("type", "fs")
                                    .add("path", "/some/path")
                                    .build()
                        ).build()
                    ).build()
                ).build().toString().getBytes()
            )
        ).join();
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.repoConfig()
                .storageOpt()
        );
    }

    @Test
    void throwsExceptionForUnknownAlias() {
        this.saveAliasConfig("some alias", StorageAliases.FILE_NAME);
        new RepoConfigYaml(RepositoriesFromStorageTest.TYPE)
            .withStorageAlias("unknown alias")
            .saveTo(this.storage, RepositoriesFromStorageTest.REPO);
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.repoConfig()
                .storageOpt()
        );
    }

    private RepoConfig repoConfig() {
        return new RepositoriesFromStorage(this.storage)
            .config(RepositoriesFromStorageTest.REPO)
            .toCompletableFuture().join();
    }

    private void saveAliasConfig(final String alias, final String filename) {
        this.storage.save(
            new Key.From(filename),
            new Content.From(
                Yaml.createYamlMappingBuilder().add(
                    "storages", Yaml.createYamlMappingBuilder()
                        .add(
                            alias, Yaml.createYamlMappingBuilder()
                                .add("type", "fs")
                                .add("path", "/some/path")
                                .build()
                        ).build()
                ).build().toString().getBytes()
            )
        ).join();
    }

}

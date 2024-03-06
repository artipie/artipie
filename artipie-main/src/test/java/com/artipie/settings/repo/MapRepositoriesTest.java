/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings.repo;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.settings.AliasSettings;
import com.artipie.settings.Settings;
import com.artipie.test.TestSettings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;

/**
 * Tests for cache of files with configuration in {@link MapRepositories}.
 */
final class MapRepositoriesTest {

    /**
     * Repo name.
     */
    private static final String REPO = "my-repo";

    /**
     * Type repository.
     */
    private static final String TYPE = "maven";

    private Storage storage;

    private Settings settings;

    @BeforeEach
    void setUp() {
        this.settings = new TestSettings();
        this.storage = this.settings.repoConfigsStorage();
    }

    @ParameterizedTest
    @CsvSource({"_storages.yaml", "_storages.yml"})
    void findRepoSettingAndCreateRepoConfigWithStorageAlias(final String filename) {
        final String alias = "default";
        new RepoConfigYaml(MapRepositoriesTest.TYPE)
            .withStorageAlias(alias)
            .saveTo(this.storage, MapRepositoriesTest.REPO);
        this.saveAliasConfig(alias, filename);
        Assertions.assertTrue(this.repoConfig().storageOpt().isPresent());
    }

    @Test
    void findRepoSettingAndCreateRepoConfigWithCustomStorage() {
        new RepoConfigYaml(MapRepositoriesTest.TYPE)
            .withFileStorage(Path.of("some", "somepath"))
            .saveTo(this.storage, MapRepositoriesTest.REPO);
        Assertions.assertTrue(this.repoConfig().storageOpt().isPresent());
    }

    @Test
    void throwsExceptionWhenConfigYamlAbsent() {
        Assertions.assertTrue(
            new MapRepositories(this.settings)
                .config(MapRepositoriesTest.REPO)
                .isEmpty()
        );
    }

    @Test
    void throwsExceptionWhenConfigYamlMalformedSinceWithoutStorage() {
        new RepoConfigYaml(MapRepositoriesTest.TYPE)
            .saveTo(this.storage, MapRepositoriesTest.REPO);
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.repoConfig().storage()
        );
    }

    @Test
    void throwsExceptionWhenAliasesConfigAbsent() {
        new RepoConfigYaml(MapRepositoriesTest.TYPE)
            .withStorageAlias("alias")
            .saveTo(this.storage, MapRepositoriesTest.REPO);
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.repoConfig().storageOpt()
        );
    }

    @Test
    void throwsExceptionWhenAliasConfigMalformedSinceSequenceInsteadMapping() {
        final String alias = "default";
        new RepoConfigYaml(MapRepositoriesTest.TYPE)
            .withStorageAlias(alias)
            .saveTo(this.storage, MapRepositoriesTest.REPO);
        this.storage.save(
            new Key.From(AliasSettings.FILE_NAME),
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
        this.saveAliasConfig("some alias", AliasSettings.FILE_NAME);
        new RepoConfigYaml(MapRepositoriesTest.TYPE)
            .withStorageAlias("unknown alias")
            .saveTo(this.storage, MapRepositoriesTest.REPO);
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.repoConfig()
                .storageOpt()
        );
    }

    @Test
    void readFromCacheAndRefreshCacheData() {
        Key key = new Key.From("some-repo.yaml");
        new BlockingStorage(this.settings.repoConfigsStorage())
            .save(key, "repo:\n  type: old_type".getBytes());
        Repositories repos = new MapRepositories(this.settings);
        new BlockingStorage(this.settings.repoConfigsStorage())
            .save(key, "repo:\n  type: new_type".getBytes());

        Assertions.assertEquals("old_type",
            repos.config(key.string()).orElseThrow().type());

        repos.refresh();

        Assertions.assertEquals("new_type",
            repos.config(key.string()).orElseThrow().type());
    }

    @Test
    void readAliasesFromCacheAndRefreshCache() {
        final Key alias = new Key.From("_storages.yaml");
        new TestResource(alias.string()).saveTo(this.settings.repoConfigsStorage());
        Key config = new Key.From("bin.yaml");
        BlockingStorage cfgStorage = new BlockingStorage(this.settings.repoConfigsStorage());
        cfgStorage.save(config, "repo:\n  type: maven\n  storage: default".getBytes());
        Repositories repo = new MapRepositories(this.settings);
        cfgStorage.save(config, "repo:\n  type: maven".getBytes());

        Assertions.assertTrue(
            repo.config(config.string())
                .orElseThrow().storageOpt().isPresent()
        );

        repo.refresh();

        Assertions.assertTrue(
            repo.config(config.string())
                .orElseThrow().storageOpt().isEmpty()
        );
    }

    private RepoConfig repoConfig() {
        return new MapRepositories(this.settings)
            .config(MapRepositoriesTest.REPO)
            .orElseThrow();
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

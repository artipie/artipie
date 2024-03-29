/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings.repo;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.asto.test.TestResource;
import com.artipie.cache.StoragesCache;
import com.artipie.http.client.RemoteConfig;
import com.artipie.settings.StorageByAlias;
import com.artipie.test.TestStoragesCache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Test for {@link RepoConfig}.
 */
public final class RepoConfigTest {

    private StoragesCache cache;

    @BeforeEach
    public void setUp() {
        this.cache = new TestStoragesCache();
    }

    @Test
    public void readsCustom() throws Exception {
        final YamlMapping yaml = readFull().settings().orElseThrow();
        Assertions.assertEquals("custom-value", yaml.string("custom-property"));
    }

    @Test
    public void failsToReadCustom() throws Exception {
        Assertions.assertTrue(readMin().settings().isEmpty());
    }

    @Test
    public void readContentLengthMax() throws Exception {
        Assertions.assertEquals(Optional.of(123L), readFull().contentLengthMax());
    }

    @Test
    void remotesPriority() throws Exception {
        List<RemoteConfig> remotes = readFull().remotes();
        Assertions.assertEquals(4, remotes.size());
        Assertions.assertEquals(new RemoteConfig(URI.create("host4.com"), 200, null, null), remotes.get(0));
        Assertions.assertEquals(new RemoteConfig(URI.create("host1.com"), 100, null, null), remotes.get(1));
        Assertions.assertEquals(new RemoteConfig(URI.create("host2.com"), 0, "test_user", "12345"), remotes.get(2));
        Assertions.assertEquals(new RemoteConfig(URI.create("host3.com"), -10, null, null), remotes.get(3));
    }

    @Test
    public void readEmptyContentLengthMax() throws Exception {
        Assertions.assertTrue(readMin().contentLengthMax().isEmpty());
    }

    @Test
    public void readsPortWhenSpecified() throws Exception {
        Assertions.assertEquals(OptionalInt.of(1234), readFull().port());
    }

    @Test
    public void readsEmptyPortWhenNotSpecified() throws Exception {
        Assertions.assertEquals(OptionalInt.empty(), readMin().port());
    }

    @Test
    public void readsRepositoryTypeRepoPart() throws Exception {
        Assertions.assertEquals("maven", readMin().type());
    }

    @Test
    public void throwExceptionWhenPathNotSpecified() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> repoCustom().path()
        );
    }

    @Test
    public void getPathPart() throws Exception {
        Assertions.assertEquals("mvn", readFull().path());
    }

    @Test
    public void getUrlWhenUrlIsCorrect() {
        final String target = "http://host:8080/correct";
        Assertions.assertEquals(target, repoCustom(target).url().toString());
    }

    @Test
    public void throwExceptionWhenUrlIsMalformed() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> repoCustom("host:8080/without/scheme").url()
        );
    }

    @Test
    public void throwsExceptionWhenStorageWithDefaultAliasesNotConfigured() {
        Assertions.assertEquals("Storage is not configured",
            Assertions.assertThrows(
                IllegalStateException.class,
                () -> repoCustom().storage()
            ).getMessage());
    }

    @Test
    public void throwsExceptionForInvalidStorageConfig() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> RepoConfig.from(
                Yaml.createYamlMappingBuilder().add(
                    "repo", Yaml.createYamlMappingBuilder()
                        .add(
                            "storage", Yaml.createYamlSequenceBuilder()
                                .add("wrong because sequence").build()
                        ).build()
                ).build(),
                new StorageByAlias(Yaml.createYamlMappingBuilder().build()),
                new Key.From("key"), cache, false
            ).storage()
        );
    }

    private RepoConfig readFull() throws Exception {
        return readFromResource("repo-full-config.yml");
    }

    private RepoConfig readMin() throws Exception {
        return readFromResource("repo-min-config.yml");
    }

    private RepoConfig repoCustom() {
        return repoCustom("http://host:8080/correct");
    }

    private RepoConfig repoCustom(final String value) {
        return RepoConfig.from(
            Yaml.createYamlMappingBuilder().add(
                "repo", Yaml.createYamlMappingBuilder()
                    .add("type", "maven")
                    .add("url", value)
                    .build()
            ).build(),
            new StorageByAlias(Yaml.createYamlMappingBuilder().build()),
            new Key.From("repo-custom.yml"), cache, false
        );
    }

    private RepoConfig readFromResource(final String name) throws IOException {
        return RepoConfig.from(
            Yaml.createYamlInput(
                new TestResource(name).asInputStream()
            ).readYamlMapping(),
            new StorageByAlias(Yaml.createYamlMappingBuilder().build()),
            new Key.From(name), cache, false
        );
    }
}

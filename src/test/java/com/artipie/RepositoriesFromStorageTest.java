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
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link RepositoriesFromStorage}.
 *
 * @since 0.14
 */
final class RepositoriesFromStorageTest {

    /**
     * Repo name.
     */
    private static final String REPO = "my-repo";

    /**
     * Type repository.
     */
    private static final String TYPE = "maven";

    @Test
    void findRepoSettingAndCreateRepoConfigWithStorageAlias() {
        final Storage storage = new InMemoryStorage();
        final String alias = "default";
        new RepoConfigYaml(RepositoriesFromStorageTest.TYPE)
            .withStorageAlias(alias)
            .saveTo(storage, RepositoriesFromStorageTest.REPO);
        storage.save(
            new Key.From("_storages.yaml"),
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
        MatcherAssert.assertThat(
            new RepositoriesFromStorage(storage)
                .config(RepositoriesFromStorageTest.REPO)
                .toCompletableFuture().join()
                .storageOpt()
                .isPresent(),
            new IsEqual<>(true)
        );
    }

    @Test
    void findRepoSettingAndCreateRepoConfigWithCustomStorage() {
        final Storage storage = new InMemoryStorage();
        new RepoConfigYaml(RepositoriesFromStorageTest.TYPE)
            .withFileStorage(Path.of("some", "somepath"))
            .saveTo(storage, RepositoriesFromStorageTest.REPO);
        MatcherAssert.assertThat(
            new RepositoriesFromStorage(storage)
                .config(RepositoriesFromStorageTest.REPO)
                .toCompletableFuture().join()
                .storageOpt()
                .isPresent(),
            new IsEqual<>(true)
        );
    }

}

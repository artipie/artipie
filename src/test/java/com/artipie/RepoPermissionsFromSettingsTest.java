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
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.cactoos.list.ListOf;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RepoPermissions.FromSettings}.
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class RepoPermissionsFromSettingsTest {

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void returnsRepoList() {
        this.storage.save(new Key.From("one.yaml"), Content.EMPTY).join();
        this.storage.save(new Key.From("two.yaml"), Content.EMPTY).join();
        this.storage.save(new Key.From("abc"), Content.EMPTY).join();
        this.storage.save(new Key.From("three.yaml"), Content.EMPTY).join();
        MatcherAssert.assertThat(
            new RepoPermissions.FromSettings(new Settings.Fake(this.storage)).repositories()
                .toCompletableFuture().join(),
            Matchers.containsInAnyOrder("one", "two", "three")
        );
    }

    @Test
    void returnsPermissionsList() {
        final String john = "john";
        final String download = "download";
        final String upload = "upload";
        final String repo = "maven";
        this.addSettings(
            repo,
            new MapOf<String, List<String>>(
                new MapEntry<>(john, new ListOf<String>(download, upload))
            )
        );
        MatcherAssert.assertThat(
            new RepoPermissions.FromSettings(new Settings.Fake(this.storage)).permissions(repo)
                .toCompletableFuture().join(),
            Matchers.hasEntry(john, new ListOf<String>(download, upload))
        );
    }

    @Test
    void returnsEmptyMapWhenPermissionsAreNotSet() {
        final String repo = "pypi";
        this.addEmpty(repo);
        MatcherAssert.assertThat(
            new RepoPermissions.FromSettings(new Settings.Fake(this.storage)).permissions(repo)
                .toCompletableFuture().join().size(),
            new IsEqual<>(0)
        );
    }

    private void addSettings(final String repo, final Map<String, List<String>> permissions) {
        YamlMappingBuilder builder = Yaml.createYamlMappingBuilder();
        for (final Map.Entry<String, List<String>> entry : permissions.entrySet()) {
            YamlSequenceBuilder perms = Yaml.createYamlSequenceBuilder();
            for (final String perm : entry.getValue()) {
                perms = perms.add(perm);
            }
            builder = builder.add(entry.getKey(), perms.build());
        }
        this.storage.save(
            new Key.From(String.format("%s.yaml", repo)),
            new Content.From(
                Yaml.createYamlMappingBuilder().add(
                    "repo",
                    Yaml.createYamlMappingBuilder().add("permissions", builder.build()).build()
                ).build().toString().getBytes(StandardCharsets.UTF_8)
            )
        ).join();
    }

    private void addEmpty(final String repo) {
        this.storage.save(
            new Key.From(String.format("%s.yaml", repo)),
            new Content.From(
                Yaml.createYamlMappingBuilder().add(
                    "repo",
                    Yaml.createYamlMappingBuilder().add("type", "file")
                        .add("storage", "default").build()
                ).build().toString().getBytes(StandardCharsets.UTF_8)
            )
        ).join();
    }

}

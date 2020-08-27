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
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class for adding repo permissions to the storage and getting them.
 * @since 0.10
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class UtilRepoPermissions {
    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Storage
     */
    public UtilRepoPermissions(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Add to the storage repo with map with users and permissions.
     * @param repo Repo name
     * @param permissions Map with users and permissions
     */
    public void addSettings(final String repo, final Map<String, List<String>> permissions) {
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
                    Yaml.createYamlMappingBuilder()
                        .add("type", "any")
                        .add("permissions", builder.build()).build()
                ).build().toString().getBytes(StandardCharsets.UTF_8)
            )
        ).join();
    }

    /**
     * Add to the storage empty repo.
     * @param repo Repo name
     */
    public void addEmpty(final String repo) {
        this.storage.save(
            new Key.From(String.format("%s.yaml", repo)),
            new Content.From(
                Yaml.createYamlMappingBuilder().add(
                    "repo",
                    Yaml.createYamlMappingBuilder()
                        .add("type", "file")
                        .add("storage", "default").build()
                ).build().toString().getBytes(StandardCharsets.UTF_8)
            )
        ).join();
    }

    /**
     * Get collection with permissions for the user.
     * @param repo Repo name
     * @param user Username
     * @return Collection with permissions for the specified user.
     * @throws IOException In case of error reading settings.
     */
    public Collection<String> permissionsForUser(final String repo, final String user)
        throws IOException {
        return this.permissionsSection(repo).yamlSequence(user)
            .values().stream().map(node -> node.asScalar().value())
            .collect(Collectors.toList());
    }

    /**
     * Get section with permissions.
     * @param repo Repo name
     * @return Yaml mapping for the specified repo with only permissions.
     * @throws IOException In case of error reading settings.
     */
    YamlMapping permissionsSection(final String repo) throws IOException {
        return this.repoSection(repo).yamlMapping("permissions");
    }

    /**
     * Get repo section.
     * @param repo Repo name
     * @return Yaml mapping for the specified repo.
     * @throws IOException In case of error reading settings.
     */
    YamlMapping repoSection(final String repo) throws IOException {
        return Yaml.createYamlInput(
            new PublisherAs(this.storage.value(new Key.From(String.format("%s.yaml", repo))).join())
                .asciiString().toCompletableFuture().join()
        ).readYamlMapping().yamlMapping("repo");
    }
}

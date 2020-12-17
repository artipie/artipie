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
import com.amihaiemil.eoyaml.YamlNode;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.management.RepoPermissions;
import com.artipie.management.api.ContentAsYaml;
import com.artipie.repo.ConfigFile;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import org.cactoos.set.SetOf;

/**
 * {@link RepoPermissions} from Artipie settings.
 * @since 0.12.2
 */
public final class RepoPermissionsFromStorage implements RepoPermissions {

    /**
     * Permissions section name.
     */
    private static final String PERMS = "permissions";

    /**
     * Permissions include patterns section in YAML settings.
     */
    private static final String INCLUDE_PATTERNS = "permissions_include_patterns";

    /**
     * Repo section in yaml settings.
     */
    private static final String REPO_SECTION = "repo";

    /**
     * Artipie settings storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Artipie settings storage
     */
    public RepoPermissionsFromStorage(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletionStage<List<String>> repositories() {
        return this.storage.list(Key.ROOT)
            .thenApply(
                list -> list.stream()
                    .filter(name -> new ConfigFile(name).isYamlOrYml())
                    .map(name -> new ConfigFile(name).name())
                    .collect(Collectors.toList())
        );
    }

    @Override
    public CompletionStage<Void> remove(final String repo) {
        final Key key = RepoPermissionsFromStorage.repoSettingsKey(repo);
        return this.repo(key).thenApply(
            mapping -> Yaml.createYamlMappingBuilder()
                .add(
                    RepoPermissionsFromStorage.REPO_SECTION,
                    RepoPermissionsFromStorage.copyRepoSection(mapping).build()
                ).build()
        ).thenCompose(
            result -> this.saveSettings(key, result)
        );
    }

    @Override
    public CompletionStage<Void> update(
        final String repo,
        final Collection<PermissionItem> permissions,
        final Collection<PathPattern> patterns) {
        final Key key = RepoPermissionsFromStorage.repoSettingsKey(repo);
        return this.repo(key).thenApply(
            mapping -> {
                YamlMappingBuilder res = RepoPermissionsFromStorage.copyRepoSection(mapping);
                YamlMappingBuilder perms = Yaml.createYamlMappingBuilder();
                if (!permissions.isEmpty()) {
                    for (final PermissionItem item : permissions) {
                        perms = perms.add(item.username(), item.yaml().build());
                    }
                    res = res.add(RepoPermissionsFromStorage.PERMS, perms.build());
                }
                if (!patterns.isEmpty()) {
                    YamlSequenceBuilder builder = Yaml.createYamlSequenceBuilder();
                    for (final PathPattern pattern : patterns) {
                        builder = builder.add(pattern.string());
                    }
                    res = res.add(
                        RepoPermissionsFromStorage.INCLUDE_PATTERNS, builder.build()
                    );
                }
                return Yaml.createYamlMappingBuilder()
                    .add(RepoPermissionsFromStorage.REPO_SECTION, res.build()).build();
            }
        ).thenCompose(
            result -> this.saveSettings(key, result)
        );
    }

    @Override
    public CompletionStage<Collection<PermissionItem>> permissions(final String repo) {
        return this.repo(RepoPermissionsFromStorage.repoSettingsKey(repo)).thenApply(
            yaml -> Optional.ofNullable(yaml.yamlMapping(RepoPermissionsFromStorage.PERMS))
        ).thenApply(
            yaml -> yaml.map(
                perms -> perms.keys().stream().map(
                    node -> new PermissionItem(
                        node.asScalar().value(),
                        perms.yamlSequence(node.asScalar().value()).values().stream()
                        .map(item -> item.asScalar().value())
                            .collect(Collectors.toList())
                    )
                ).collect(Collectors.toList())
            ).orElse(Collections.emptyList())
        );
    }

    @Override
    public CompletionStage<Collection<PathPattern>> patterns(final String repo) {
        return this.repo(RepoPermissionsFromStorage.repoSettingsKey(repo)).thenApply(
            yaml -> Optional
                .ofNullable(yaml.yamlSequence(RepoPermissionsFromStorage.INCLUDE_PATTERNS))
        ).thenApply(
            yaml -> yaml.map(
                seq -> seq.values().stream()
                    .map(value -> value.asScalar().value())
                    .map(PathPattern::new)
                    .collect(Collectors.toList())
            ).orElse(Collections.emptyList())
        );
    }

    /**
     * Repo sections from settings.
     * @param key Repo settings key
     * @return Completion action with yaml repo section
     */
    private CompletionStage<YamlMapping> repo(final Key key) {
        return new RxStorageWrapper(this.storage)
            .value(key)
            .to(new ContentAsYaml())
            .map(yaml -> yaml.yamlMapping(RepoPermissionsFromStorage.REPO_SECTION))
            .to(SingleInterop.get());
    }

    /**
     * Saves changed settings to storage.
     * @param key Key to save storage item by
     * @param result Yaml result
     * @return Completable operation
     */
    private CompletableFuture<Void> saveSettings(final Key key, final YamlMapping result) {
        return this.storage.save(
            key, new Content.From(result.toString().getBytes(StandardCharsets.UTF_8))
        );
    }

    /**
     * Repo settings key.
     * @param repo Repo name
     * @return Settings key
     */
    private static Key repoSettingsKey(final String repo) {
        return new Key.From(String.format("%s.yaml", repo));
    }

    /**
     * Copy `repo` section without permissions from existing yaml setting.
     * @param mapping Repo section mapping
     * @return Setting without permissions
     */
    private static YamlMappingBuilder copyRepoSection(final YamlMapping mapping) {
        YamlMappingBuilder res = Yaml.createYamlMappingBuilder();
        final List<YamlNode> keep = mapping.keys().stream()
            .filter(
                node -> !new SetOf<>(
                    RepoPermissionsFromStorage.PERMS,
                    RepoPermissionsFromStorage.INCLUDE_PATTERNS
                ).contains(node.asScalar().value())
            )
            .collect(Collectors.toList());
        for (final YamlNode node : keep) {
            res = res.add(node, mapping.value(node));
        }
        return res;
    }
}

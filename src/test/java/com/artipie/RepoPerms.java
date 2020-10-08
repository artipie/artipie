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
import com.amihaiemil.eoyaml.YamlSequence;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Class for generating repo permissions.
 * @since 0.10
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class RepoPerms {
    /**
     * Permissions section name.
     */
    private static final String PERMISSIONS = "permissions";

    /**
     * Collection with user permissions.
     */
    private final Collection<RepoPermissions.PermissionItem> usersperms;

    /**
     * Collection of included patterns.
     */
    private final Collection<String> patterns;

    /**
     * Ctor.
     */
    public RepoPerms() {
        this(Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Ctor.
     * @param user Username
     * @param action Action
     */
    public RepoPerms(final String user, final String action) {
        this(new RepoPermissions.PermissionItem(user, action));
    }

    /**
     * Ctor.
     * @param user Username
     * @param actions Actions
     */
    public RepoPerms(final String user, final List<String> actions) {
        this(new RepoPermissions.PermissionItem(user, actions));
    }

    /**
     * Ctor.
     * @param userperm Permission for a single user
     */
    public RepoPerms(final RepoPermissions.PermissionItem userperm) {
        this(Collections.singleton(userperm));
    }

    /**
     * Primary ctor.
     * @param usersperms Collection with user permissions
     */
    public RepoPerms(final Collection<RepoPermissions.PermissionItem> usersperms) {
        this(usersperms, Collections.emptyList());
    }

    /**
     * Primary ctor.
     * @param usersperms Collection with user permissions
     * @param patterns Collection of included patterns.
     */
    public RepoPerms(
        final Collection<RepoPermissions.PermissionItem> usersperms,
        final Collection<String> patterns
    ) {
        this.usersperms = usersperms;
        this.patterns = patterns;
    }

    /**
     * Save to the storage repo with map with users and permissions.
     * @param storage Storage
     * @param repo Repo
     */
    public void saveSettings(final Storage storage, final String repo) {
        final YamlMappingBuilder root = RepoPerms.repoSectionAsBuilder(
            Yaml.createYamlMappingBuilder().add("type", "any").build()
        );
        storage.save(
            new Key.From(String.format("%s.yaml", repo)),
            new Content.From(
                Yaml.createYamlMappingBuilder().add(
                    "repo",
                    root.add(
                        RepoPerms.PERMISSIONS, this.permsYaml()
                    ).add(
                        "permissions_include_patterns", this.patternsYaml()
                    ).build()
                ).build().toString().getBytes(StandardCharsets.UTF_8)
            )
        ).join();
    }

    /**
     * Build YAML sequence of patterns.
     *
     * @return YAML sequence of patterns.
     */
    public YamlSequence patternsYaml() {
        YamlSequenceBuilder builder = Yaml.createYamlSequenceBuilder();
        for (final String pattern : this.patterns) {
            builder = builder.add(pattern);
        }
        return builder.build();
    }

    /**
     * YamlMapping with user permissions.
     * @return YamlMapping with user permissions.
     */
    public YamlMapping permsYaml() {
        YamlMappingBuilder perms = Yaml.createYamlMappingBuilder();
        if (!this.usersperms.isEmpty()) {
            for (final RepoPermissions.PermissionItem user : this.usersperms) {
                perms = perms.add(user.username(), user.yaml().build());
            }
        }
        return perms.build();
    }

    /**
     * Copy `repo` section from existing yaml setting.
     * @param mapping Repo section mapping
     * @return YamlMappingBuilder from existing yaml setting.
     */
    private static YamlMappingBuilder repoSectionAsBuilder(final YamlMapping mapping) {
        YamlMappingBuilder res = Yaml.createYamlMappingBuilder();
        final List<YamlNode> nodes = new ArrayList<>(mapping.keys());
        for (final YamlNode node : nodes) {
            res = res.add(node, mapping.value(node));
        }
        return res;
    }
}

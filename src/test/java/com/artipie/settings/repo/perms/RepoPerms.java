/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo.perms;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlSequence;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Class for generating repo permissions.
 * @since 0.10
 */
public final class RepoPerms {

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
}

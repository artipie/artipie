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
import java.util.Objects;

/**
 * Class for generating repo permissions.
 * @since 0.10
 */
public final class RepoPerms {

    /**
     * Collection with user permissions.
     */
    private final Collection<PermissionItem> usersperms;

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
        this(new PermissionItem(user, action));
    }

    /**
     * Ctor.
     * @param user Username
     * @param actions Actions
     */
    public RepoPerms(final String user, final List<String> actions) {
        this(new PermissionItem(user, actions));
    }

    /**
     * Ctor.
     * @param userperm Permission for a single user
     */
    public RepoPerms(final PermissionItem userperm) {
        this(Collections.singleton(userperm));
    }

    /**
     * Primary ctor.
     * @param usersperms Collection with user permissions
     */
    public RepoPerms(final Collection<PermissionItem> usersperms) {
        this(usersperms, Collections.emptyList());
    }

    /**
     * Primary ctor.
     * @param usersperms Collection with user permissions
     * @param patterns Collection of included patterns.
     */
    public RepoPerms(
        final Collection<PermissionItem> usersperms,
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
            for (final PermissionItem user : this.usersperms) {
                perms = perms.add(user.username(), user.yaml().build());
            }
        }
        return perms.build();
    }

    /**
     * User permission item.
     * @since 0.1
     */
    public static final class PermissionItem {

        /**
         * Username.
         */
        private final String name;

        /**
         * Permissions list.
         */
        private final List<String> perms;

        /**
         * Ctor.
         * @param name Username
         * @param permissions Permissions
         */
        public PermissionItem(final String name, final List<String> permissions) {
            this.name = name;
            this.perms = permissions;
        }

        /**
         * Ctor.
         * @param name Username
         * @param permission Permission
         */
        public PermissionItem(final String name, final String permission) {
            this(name, Collections.singletonList(permission));
        }

        /**
         * Get username.
         * @return String username
         */
        public String username() {
            return this.name;
        }

        /**
         * Get permissions list.
         * @return List of permissions
         */
        public List<String> permissions() {
            return this.perms;
        }

        @Override
        public boolean equals(final Object other) {
            final boolean res;
            if (this == other) {
                res = true;
            } else if (other == null || getClass() != other.getClass()) {
                res = false;
            } else {
                final PermissionItem that = (PermissionItem) other;
                res = Objects.equals(this.name, that.name)
                    && Objects.equals(this.perms, that.perms);
            }
            return res;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.perms);
        }

        /**
         * Permissions yaml sequence.
         * @return Yaml permissions sequence builder
         */
        public YamlSequenceBuilder yaml() {
            YamlSequenceBuilder res = Yaml.createYamlSequenceBuilder();
            for (final String item : this.perms) {
                res = res.add(item);
            }
            return res;
        }
    }
}

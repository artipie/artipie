/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo.perms;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * Repository permissions settings.
 * @since 0.1
 */
public interface RepoPermissions {

    /**
     * Artipie repositories list.
     * @return Repository names list
     */
    CompletionStage<List<String>> repositories();

    /**
     * Deletes all permissions for repository.
     * @param repo Repository name
     * @return Completion remove action
     */
    CompletionStage<Void> remove(String repo);

    /**
     * Adds or updates repository permissions.
     * @param repo Repository name
     * @param permissions Permissions list
     * @param patterns Included path patterns
     * @return Completion action
     */
    CompletionStage<Void> update(
        String repo,
        Collection<PermissionItem> permissions,
        Collection<PathPattern> patterns
    );

    /**
     * Get repository permissions settings, returns users permissions list.
     * @param repo Repository name
     * @return Completion action with map with users and permissions
     */
    CompletionStage<Collection<PermissionItem>> permissions(String repo);

    /**
     * Read included path patterns.
     *
     * @param repo Repository name
     * @return Collection of included path patterns
     */
    CompletionStage<Collection<PathPattern>> patterns(String repo);

    /**
     * User permission item.
     * @since 0.1
     */
    final class PermissionItem {

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

    /**
     * Represents path pattern used to check permissions inside repository.
     * Specified by expression in Ant-like syntax, example: "/path/**&#47;*.txt"
     *
     * @since 0.1
     */
    final class PathPattern {

        /**
         * Pattern expression.
         */
        private final String expr;

        /**
         * Ctor.
         *
         * @param expr Pattern expression.
         */
        public PathPattern(final String expr) {
            this.expr = expr;
        }

        /**
         * Get pattern expression.
         *
         * @return Pattern expression string.
         */
        public String string() {
            return this.expr;
        }

        /**
         * Check that pattern is valid.
         *
         * @param repo Repository name.
         * @return True if valid, false - otherwise
         */
        public boolean valid(final String repo) {
            return this.expr.matches(String.format("(%s/)?(\\*\\*)*(/\\*)?", repo));
        }
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.RepoName;
import com.artipie.docker.perms.DockerActions;
import com.artipie.docker.perms.RegistryCategory;
import com.artipie.security.perms.Action;

/**
 * Operation scope described in Docker Registry auth specification.
 * Scope is an authentication scope for performing an action on resource.
 * See <a href="https://docs.docker.com/registry/spec/auth/scope/">Token Scope Documentation</a>.
 *
 * @since 0.10
 */
public interface Scope {

    /**
     * Get resource type.
     *
     * @return Resource type.
     */
    String type();

    /**
     * Get resource name.
     *
     * @return Resource name.
     */
    String name();

    /**
     * Get resource action.
     *
     * @return Resource action.
     */
    Action action();

    /**
     * Get scope as string in default format. See
     * <a href="https://docs.docker.com/registry/spec/auth/scope/">Token Scope Documentation</a>.
     *
     * @return Scope string.
     */
    default String string() {
        return String.format("%s:%s:%s", this.type(), this.name(), this.action());
    }

    /**
     * Abstract decorator for scope.
     *
     * @since 0.10
     */
    abstract class Wrap implements Scope {

        /**
         * Origin scope.
         */
        private final Scope scope;

        /**
         * Ctor.
         *
         * @param scope Origin scope.
         */
        public Wrap(final Scope scope) {
            this.scope = scope;
        }

        @Override
        public final String type() {
            return this.scope.type();
        }

        @Override
        public final String name() {
            return this.scope.name();
        }

        @Override
        public final Action action() {
            return this.scope.action();
        }
    }

    /**
     * Scope for action on repository type resource.
     *
     * @since 0.10
     */
    final class Repository implements Scope {

        /**
         * Resource name.
         */
        private final RepoName name;

        /**
         * Resource action.
         */
        private final DockerActions action;

        /**
         * Ctor.
         *
         * @param name Resource name.
         * @param action Resource action.
         */
        public Repository(final RepoName name, final DockerActions action) {
            this.name = name;
            this.action = action;
        }

        @Override
        public String type() {
            return "repository";
        }

        @Override
        public String name() {
            return this.name.value();
        }

        @Override
        public DockerActions action() {
            return this.action;
        }

        /**
         * Scope for pull action on repository resource.
         *
         * @since 0.10
         */
        static final class Pull extends Wrap {

            /**
             * Ctor.
             *
             * @param name Resource name.
             */
            Pull(final RepoName name) {
                super(new Repository(name, DockerActions.PULL));
            }
        }

        /**
         * Scope for push action on repository resource.
         *
         * @since 0.10
         */
        static final class Push extends Wrap {

            /**
             * Ctor.
             *
             * @param name Resource name.
             */
            Push(final RepoName name) {
                super(new Repository(name, DockerActions.PUSH));
            }
        }

        /**
         * Scope for push action on repository resource.
         *
         * @since 0.12
         */
        static final class OverwriteTags extends Wrap {

            /**
             * Ctor.
             *
             * @param name Resource name.
             */
            OverwriteTags(final RepoName name) {
                super(new Repository(name, DockerActions.OVERWRITE));
            }
        }
    }

    /**
     * Scope for action on registry type resource, such as reading repositories catalog.
     *
     * @since 0.11
     */
    final class Registry implements Scope {

        /**
         * Resource action.
         */
        private final RegistryCategory category;

        /**
         * Ctor.
         *
         * @param category Resource category.
         */
        public Registry(final RegistryCategory category) {
            this.category = category;
        }

        @Override
        public String type() {
            return "registry";
        }

        @Override
        public String name() {
            return "*";
        }

        @Override
        public RegistryCategory action() {
            return this.category;
        }
    }
}

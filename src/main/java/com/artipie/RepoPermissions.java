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

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Repository permissions settings.
 * @since 0.10
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
     * Adds or updates repository permission.
     * @param repo Repository name
     * @param username Username
     * @param permission Permission name
     * @return Completion action
     */
    CompletionStage<Void> addUpdate(String repo, String username, String permission);

    /**
     * Get repository permissions settings.
     * @param repo Repository name
     * @return Completion action with map with users and permissions
     */
    CompletionStage<Map<String, List<String>>> get(String repo);

    /**
     * {@link RepoPermissions} from Artipie settings.
     * @since 0.10
     */
    final class FromSettings implements RepoPermissions {

        /**
         * Artipie settings.
         */
        private final Settings settings;

        /**
         * Ctor.
         * @param settings Artipie settings
         */
        public FromSettings(final Settings settings) {
            this.settings = settings;
        }

        @Override
        public CompletionStage<List<String>> repositories() {
            return this.storage().list(Key.ROOT)
                .thenApply(
                    list -> list.stream()
                        .map(Key::string)
                        .filter(key -> key.contains("yaml"))
                        .map(key -> key.replace(".yaml", ""))
                        .collect(Collectors.toList())
            );
        }

        @Override
        public CompletionStage<Void> remove(final String repo) {
            throw new UnsupportedOperationException("To be implemented");
        }

        @Override
        public CompletionStage<Void> addUpdate(final String repo, final String username,
            final String permission) {
            throw new UnsupportedOperationException("Not implemented yet");
        }

        @Override
        public CompletionStage<Map<String, List<String>>> get(final String repo) {
            throw new UnsupportedOperationException("To implemented later");
        }

        /**
         * Get storage from settings.
         * @return Storage instance
         */
        private Storage storage() {
            try {
                return this.settings.storage();
            } catch (final IOException err) {
                throw new UncheckedIOException(err);
            }
        }
    }

}

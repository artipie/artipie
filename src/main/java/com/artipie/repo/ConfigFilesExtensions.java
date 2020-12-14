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
package com.artipie.repo;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Supporting several config files extensions (e.g. `.yaml` and `.yml`).
 *
 * @since 0.14
 */
public final class ConfigFilesExtensions {

    /**
     * File name without extension.
     */
    private final String filename;

    /**
     * Ctor.
     * @param filename Filename without extension
     */
    public ConfigFilesExtensions(final String filename) {
        this.filename = filename;
    }

    /**
     * Does file exist in the specified storage?
     * @param storage Storage where the file with different extensions is checked for existence
     * @return True if a file with either of the two extensions exists, false otherwise.
     */
    public CompletionStage<Boolean> exists(final Storage storage) {
        final Key yaml = new Key.From(String.format("%s.yaml", this.filename));
        return storage.exists(yaml)
            .thenCompose(
                exist -> {
                    final CompletionStage<Boolean> result;
                    if (exist) {
                        result = CompletableFuture.completedFuture(true);
                    } else {
                        final Key yml = new Key.From(String.format("%s.yml", this.filename));
                        result = storage.exists(yml);
                    }
                    return result;
                }
            );
    }

    /**
     * Obtains contents from the specified storage.
     * @return Content of the file.
     */
    public CompletableFuture<Content> value() {
        throw new UnsupportedOperationException();
    }

    /**
     * Trim the file name extension.
     * @since 0.14
     */
    public static final class Trim {

        /**
         * Filename with extension.
         */
        private final String name;

        /**
         * Ctor.
         *
         * @param name Filename with extension
         */
        public Trim(final String name) {
            this.name = name;
        }

        /**
         * Trim name of the config file.
         * @return Filename without extensions if a filename ends with either of
         *  the two extensions, otherwise empty.
         */
        public Optional<String> value() {
            final String yaml = ".yaml";
            final String yml = ".yml";
            final Optional<String> result;
            if (this.name.endsWith(yaml)) {
                result = Optional.of(this.name.substring(0, this.name.length() - yaml.length()));
            } else if (this.name.endsWith(yml)) {
                result = Optional.of(this.name.substring(0, this.name.length() - yml.length()));
            } else {
                result = Optional.empty();
            }
            return result;
        }
    }

}

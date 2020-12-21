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
import java.util.concurrent.CompletionStage;

/**
 * Wrapper for {@link ConfigFile} for `management-api` module. It allows
 * to use the logic of working with two extensions in another module.
 * @since 0.14
 */
public final class ConfigFileApi {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Storage
     */
    public ConfigFileApi(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Does specified config file exist in the storage?
     * @param filename Filename
     * @return True if a file with either of the two extensions exists, false otherwise.
     */
    public CompletionStage<Boolean> exists(final Key filename) {
        return new ConfigFile(filename).existsIn(this.storage);
    }

    /**
     * Obtains contents of the config file from the storage.
     * @param filename Filename
     * @return Content of the config file.
     */
    public CompletionStage<Content> value(final Key filename) {
        return new ConfigFile(filename).valueFrom(this.storage);
    }

    /**
     * Filename.
     * @param filename Filename
     * @return Filename without extension.
     */
    static String name(final Key filename) {
        return new ConfigFile(filename).name();
    }

    /**
     * Extension.
     * @param filename Filename
     * @return Extension if present, empty otherwise.
     */
    static Optional<String> extension(final Key filename) {
        return new ConfigFile(filename).extension();
    }

    /**
     * Is `yaml` or `yml` file?
     * @param filename Filename
     * @return True if is the file with `yaml` or `yml` extension, false otherwise.
     */
    static boolean isYamlOrYml(final Key filename) {
        return new ConfigFile(filename).isYamlOrYml();
    }

}

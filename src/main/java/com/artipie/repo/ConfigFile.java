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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Supporting several config files extensions (e.g. `.yaml` and `.yml`).
 *
 * @since 0.14
 */
public final class ConfigFile {

    /**
     * Pattern to divide filename into two groups: name and extension.
     */
    private static final Pattern PTN = Pattern.compile("(?<name>.*)(\\.yaml|\\.yml)$");

    /**
     * Filename.
     */
    private final String filename;

    /**
     * Ctor.
     * @param filename Filename
     */
    public ConfigFile(final String filename) {
        this.filename = filename;
    }

    /**
     * Ctor.
     * @param filename Filename
     */
    public ConfigFile(final Key filename) {
        this(filename.string());
    }

    /**
     * Does file exist in the specified storage?
     * @param storage Storage where the file with different extensions is checked for existence
     * @return True if a file with either of the two extensions exists, false otherwise.
     */
    public CompletionStage<Boolean> existsIn(final Storage storage) {
        final String name = this.trimExtension().orElse(this.filename);
        final Key yaml = new Key.From(String.format("%s.yaml", name));
        return storage.exists(yaml)
            .thenCompose(
                exist -> {
                    final CompletionStage<Boolean> result;
                    if (exist) {
                        result = CompletableFuture.completedFuture(true);
                    } else {
                        final Key yml = new Key.From(String.format("%s.yml", name));
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
    public CompletableFuture<Content> valueFrom() {
        throw new UnsupportedOperationException();
    }

    /**
     * Trim name of the config file.
     * @return Filename without extensions if a filename ends with either of
     *  the two extensions, otherwise empty.
     */
    public Optional<String> trimExtension() {
        final Optional<String> result;
        final Matcher matcher = PTN.matcher(this.filename);
        if (matcher.matches()) {
            result = Optional.of(matcher.group("name"));
        } else {
            result = Optional.empty();
        }
        return result;
    }

}

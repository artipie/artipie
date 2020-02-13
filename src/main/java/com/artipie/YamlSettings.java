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
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Settings built from YAML.
 *
 * @since 0.1
 */
public final class YamlSettings implements Settings {

    /**
     * YAML file content.
     */
    private final String content;

    /**
     * Ctor.
     *
     * @param content YAML file content.
     */
    public YamlSettings(final String content) {
        this.content = content;
    }

    @Override
    public Storage storage() throws IOException {
        final YamlMapping yaml = this.storageYaml();
        final String type = string(yaml, "type");
        if (type.equals("fs")) {
            return new FileStorage(Path.of(string(yaml, "path")));
        }
        throw new IllegalStateException(String.format("Unsupported storage type: '%s'", type));
    }

    /**
     * Read storage section from YAML.
     *
     * @return Storage section.
     * @throws IOException In case of problems with reading YAML from source.
     */
    private YamlMapping storageYaml() throws IOException {
        return mapping(
            mapping(
                Yaml.createYamlInput(this.content).readYamlMapping(),
                "meta"
            ),
            "storage"
        );
    }

    /**
     * Gets mapping by key from YAML, fails if no such key exists.
     *
     * @param yaml YAML to take the value from.
     * @param key Key to take value by.
     * @return Value found by key.
     */
    private static YamlMapping mapping(final YamlMapping yaml, final String key) {
        final YamlMapping mapping = yaml.yamlMapping(key);
        if (mapping == null) {
            throw new IllegalArgumentException(
                String.format("Cannot find '%s' mapping:\n%s", key, yaml)
            );
        }
        return mapping;
    }

    /**
     * Gets string by key from YAML, fails if no such key exists.
     *
     * @param yaml YAML to take the value from.
     * @param key Key to take value by.
     * @return Value found by key.
     */
    private static String string(final YamlMapping yaml, final String key) {
        final String string = yaml.string(key);
        if (string == null) {
            throw new IllegalArgumentException(
                String.format("Cannot find '%s' string:\n%s", key, yaml)
            );
        }
        return string;
    }
}

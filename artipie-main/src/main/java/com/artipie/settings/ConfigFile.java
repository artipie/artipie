/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings;

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
 * Files with two different extensions are interpreted in the same way.
 * For example, if `name.yaml` is searched in the storage then
 * files `name.yaml` and `name.yml` are searched.
 *
 * @since 0.14
 */
public final class ConfigFile {

    /**
     * Pattern to divide `yaml` or `yml` filename into two groups: name and extension.
     */
    private static final Pattern PTN_YAML = Pattern.compile("(?<name>.*)(\\.yaml|\\.yml)$");

    /**
     * Pattern to divide all filenames into two groups: name and extension.
     */
    private static final Pattern PTN_ALL =
        Pattern.compile("(?<name>.+?)(?<extension>(?<!/)\\.[^./]*$|$)");

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
     * Does config file exist in the specified storage?
     * @param storage Storage where the file with different extensions is checked for existence
     * @return True if a file with either of the two extensions exists, false otherwise.
     */
    public CompletionStage<Boolean> existsIn(final Storage storage) {
        final CompletionStage<Boolean> res;
        if (this.isYamlOrYml() || this.extension().isEmpty()) {
            final String name = this.name();
            final Key yaml = Extension.YAML.key(name);
            res = storage.exists(yaml)
                .thenCompose(
                    exist -> {
                        final CompletionStage<Boolean> result;
                        if (exist) {
                            result = CompletableFuture.completedFuture(true);
                        } else {
                            final Key yml = Extension.YML.key(name);
                            result = storage.exists(yml);
                        }
                        return result;
                    }
                );
        } else {
            res = CompletableFuture.completedFuture(false);
        }
        return res;
    }

    /**
     * Deletes value from the storage.
     * @param storage Storage where the file with different extensions is checked for existence
     * @return Result of completion.
     */
    public CompletionStage<Void> delete(final Storage storage) {
        final CompletionStage<Void> res;
        if (this.isYamlOrYml() || this.extension().isEmpty()) {
            final String name = this.name();
            final Key yaml = Extension.YAML.key(name);
            res = storage.exists(yaml)
                .thenCompose(
                    exist -> {
                        final CompletionStage<Void> result;
                        if (exist) {
                            result = storage.delete(yaml);
                        } else {
                            result = storage.delete(Extension.YML.key(name));
                        }
                        return result;
                    }
                );
        } else {
            res = CompletableFuture.allOf();
        }
        return res;
    }

    /**
     * Obtains contents from the specified storage. If files with both extensions
     * exists, the file with `.yaml` extension will be obtained.
     * @param storage Storage from which the file is obtained
     * @return Content of the file.
     */
    public CompletionStage<Content> valueFrom(final Storage storage) {
        if (!(this.isYamlOrYml() || this.extension().isEmpty())) {
            throw new IllegalStateException(
                String.format(
                    "Filename `%s` should have `.yaml` or `.yml` extension or be without extension",
                    this.filename
                )
            );
        }
        final String name = this.name();
        final Key yaml = Extension.YAML.key(name);
        return storage.exists(yaml)
            .thenCompose(
                exists -> {
                    final CompletionStage<Content> result;
                    if (exists) {
                        result = storage.value(yaml);
                    } else {
                        final Key yml = Extension.YML.key(name);
                        result = storage.value(yml);
                    }
                    return result;
                }
            );
    }

    /**
     * Is `yaml` or `yml` file?
     * @return True if is the file with `yaml` or `yml` extension, false otherwise.
     */
    public boolean isYamlOrYml() {
        return PTN_YAML.matcher(this.filename).matches();
    }

    /**
     * Filename.
     * @return Filename without extension.
     */
    public String name() {
        return this.matcher("name");
    }

    /**
     * Is system file.
     * @return True if it's a system configuration file
     */
    public boolean isSystem() {
        return this.filename.length() > 0 && this.filename.charAt(0) == '_';
    }

    /**
     * Extension.
     * @return Extension if present, empty otherwise.
     */
    public Optional<String> extension() {
        final Optional<String> extnsn;
        final String val = this.matcher("extension");
        if (val.isEmpty()) {
            extnsn = Optional.empty();
        } else {
            extnsn = Optional.of(val);
        }
        return extnsn;
    }

    /**
     * Matcher.
     * @param group Matcher group name
     * @return Value for specified group name.
     */
    private String matcher(final String group) {
        final Matcher matcher = PTN_ALL.matcher(this.filename);
        if (!matcher.matches()) {
            throw new IllegalStateException(
                String.format("Failed to get name from string `%s`", this.filename)
            );
        }
        return matcher.group(group);
    }

    /**
     * Config files extensions.
     */
    public enum Extension {
        /**
         * YAML.
         */
        YAML(".yaml"),

        /**
         * YML.
         */
        YML(".yml");

        /**
         * Extension.
         */
        private final String extension;

        /**
         * Ctor.
         * @param extension Extension
         */
        Extension(final String extension) {
            this.extension = extension;
        }

        /**
         * Extension value.
         * @return Extension.
         */
        public String value() {
            return this.extension;
        }

        /**
         * Key.
         * @param name Filename
         * @return Key from filename and extension.
         */
        public Key key(final String name) {
            return new Key.From(String.format("%s%s", name, this.extension));
        }
    }

}

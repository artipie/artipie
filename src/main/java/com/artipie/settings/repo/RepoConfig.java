/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.settings.repo;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.auth.YamlPermissions;
import com.artipie.http.auth.Permissions;
import com.artipie.micrometer.MicrometerStorage;
import com.artipie.settings.StorageAliases;
import com.artipie.settings.StorageYamlConfig;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

/**
 * Repository config.
 * @since 0.2
 * @checkstyle ParameterNumberCheck (500 lines)
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public final class RepoConfig {

    /**
     * Storages.
     */
    private final StorageAliases storages;

    /**
     * Storage prefix.
     */
    private final Key prefix;

    /**
     * Source yaml future.
     */
    private final YamlMapping yaml;

    /**
     * Are metrics enabled?
     */
    private final boolean metrics;

    /**
     * Ctor.
     * @param storages Repository storage aliases
     * @param prefix Storage prefix
     * @param yaml Config yaml
     * @param metrics Are metrics enabled?
     */
    public RepoConfig(
        final StorageAliases storages, final Key prefix, final YamlMapping yaml,
        final boolean metrics
    ) {
        this.prefix = prefix;
        this.yaml = yaml;
        this.storages = storages;
        this.metrics = metrics;
    }

    /**
     * Ctor for test usage only.
     * @param storages Repository storage aliases
     * @param prefix Storage prefix
     * @param yaml Config yaml
     */
    public RepoConfig(
        final StorageAliases storages, final Key prefix, final YamlMapping yaml
    ) {
        this(storages, prefix, yaml, false);
    }

    /**
     * Repository name.
     *
     * @return Name string.
     */
    public String name() {
        return this.prefix.string();
    }

    /**
     * Repository type.
     * @return Async string of type
     */
    public String type() {
        return this.string("type");
    }

    /**
     * Repository port.
     *
     * @return Repository port.
     */
    public OptionalInt port() {
        return Stream.ofNullable(this.repoConfig().string("port"))
            .mapToInt(Integer::parseInt)
            .findFirst();
    }

    /**
     * Repository path.
     * @return Async string of path
     */
    public String path() {
        return this.string("path");
    }

    /**
     * Repository URL.
     *
     * @return Async string of URL
     */
    public URL url() {
        final String str = this.string("url");
        try {
            return new URL(str);
        } catch (final MalformedURLException ex) {
            throw new IllegalArgumentException(
                String.format("Failed to build URL from '%s'", str),
                ex
            );
        }
    }

    /**
     * Read maximum allowed Content-Length value for incoming requests.
     *
     * @return Maximum allowed value, empty if none specified.
     */
    public Optional<Long> contentLengthMax() {
        return this.stringOpt("content-length-max").map(Long::valueOf);
    }

    /**
     * Storage.
     * @return Async storage for repo
     */
    public Storage storage() {
        return this.storageOpt().orElseThrow(
            () -> new IllegalStateException("Storage is not configured")
        );
    }

    /**
     * Create storage if configured in given YAML.
     *
     * @return Async storage for repo
     */
    public Optional<Storage> storageOpt() {
        return Optional.ofNullable(this.repoConfig().value("storage")).map(
            node -> new StorageYamlConfig(node, this.storages).subStorage(this.prefix)
        ).map(
            asto -> {
                if (this.metrics) {
                    asto = new MicrometerStorage(asto);
                }
                return asto;
            }
        );
    }

    /**
     * Custom repository configuration.
     * @return Async custom repository config or Optional.empty
     */
    public Optional<YamlMapping> settings() {
        return Optional.ofNullable(this.repoConfig().yamlMapping("settings"));
    }

    /**
     * Repository permissions.
     * @return Async permissions, empty if not configured.
     */
    public Optional<Permissions> permissions() {
        return Optional.ofNullable(this.repoConfig().yamlMapping("permissions"))
            .map(YamlPermissions::new);
    }

    /**
     * Storage aliases.
     * @return Returns {@link StorageAliases} instance
     */
    public StorageAliases storageAliases() {
        return this.storages;
    }

    /**
     * Repo part of YAML.
     *
     * @return Async YAML mapping
     */
    public YamlMapping repoConfig() {
        return Optional.ofNullable(this.yaml.yamlMapping("repo")).orElseThrow(
            () -> new IllegalStateException("Invalid repo configuration")
        );
    }

    @Override
    public String toString() {
        return this.yaml.toString();
    }

    /**
     * Reads string by key from repo part of YAML.
     *
     * @param key String key.
     * @return String value.
     */
    private String string(final String key) {
        return this.stringOpt(key).orElseThrow(
            () -> new IllegalStateException(String.format("yaml repo.%s is absent", key))
        );
    }

    /**
     * Reads string by key from repo part of YAML.
     *
     * @param key String key.
     * @return String value, empty if none present.
     */
    private Optional<String> stringOpt(final String key) {
        return Optional.ofNullable(this.repoConfig().string(key));
    }
}

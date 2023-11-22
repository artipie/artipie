/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.settings.repo;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.asto.LoggingStorage;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.micrometer.MicrometerStorage;
import com.artipie.settings.StorageByAlias;
import com.artipie.settings.cache.StoragesCache;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * Repository config.
 * @since 0.2
 * @checkstyle ParameterNumberCheck (500 lines)
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public final class RepoConfig {

    /**
     * Storage aliases.
     */
    private final StorageByAlias aliases;

    /**
     * Storage prefix.
     */
    private final Key prefix;

    /**
     * Source yaml future.
     */
    private final YamlMapping yaml;

    /**
     * Storages cache.
     */
    private final StoragesCache cache;

    /**
     * Are metrics enabled?
     */
    private final boolean metrics;

    /**
     * Ctor.
     *
     * @param aliases Repository storage aliases
     * @param prefix Storage prefix
     * @param yaml Config yaml
     * @param cache Storages cache.
     * @param metrics Are metrics enabled?
     */
    public RepoConfig(
        final StorageByAlias aliases,
        final Key prefix,
        final YamlMapping yaml,
        final StoragesCache cache,
        final boolean metrics
    ) {
        this.aliases = aliases;
        this.prefix = prefix;
        this.yaml = yaml;
        this.cache = cache;
        this.metrics = metrics;
    }

    /**
     * Ctor for test usage only.
     * @param aliases Repository storage aliases
     * @param prefix Storage prefix
     * @param yaml Config yaml
     * @param cache Storages cache.
     */
    public RepoConfig(
        final StorageByAlias aliases,
        final Key prefix,
        final YamlMapping yaml,
        final StoragesCache cache
    ) {
        this(aliases, prefix, yaml, cache, false);
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
        return Stream.ofNullable(this.repoYaml().string("port"))
            .mapToInt(Integer::parseInt)
            .findFirst();
    }

    /**
     * Start repo on http3 version?
     * @return True if so
     * @checkstyle MethodNameCheck (5 lines)
     */
    public boolean startOnHttp3() {
        return Boolean.parseBoolean(this.repoYaml().string("http3"));
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
            return URI.create(str).toURL();
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
        return Optional.ofNullable(
            this.repoYaml().value("storage")
        ).map(
            node -> new SubStorage(
                this.prefix,
                new LoggingStorage(
                    Level.INFO,
                    this.cache.storage(this.aliases, node)
                )
            )
        ).map(
            asto -> {
                Storage res = asto;
                if (this.metrics) {
                    res = new MicrometerStorage(asto);
                }
                return res;
            }
        );
    }

    /**
     * Custom repository configuration.
     *
     * @return Async custom repository config or Optional.empty
     */
    public Optional<YamlMapping> settings() {
        return Optional.ofNullable(this.repoYaml().yamlMapping("settings"));
    }

    /**
     * Storage aliases.
     *
     * @return Returns {@link StorageByAlias} instance
     */
    public StorageByAlias storageAliases() {
        return this.aliases;
    }

    /**
     * Gets storages cache.
     *
     * @return Storages cache.
     */
    public StoragesCache storagesCache() {
        return this.cache;
    }

    /**
     * Repo part of YAML.
     *
     * @return Async YAML mapping
     */
    public YamlMapping repoYaml() {
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
        return Optional.ofNullable(this.repoYaml().string(key));
    }
}

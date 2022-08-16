/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.settings.repo;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.MeasuredStorage;
import com.artipie.asto.Concatenation;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.auth.YamlPermissions;
import com.artipie.http.auth.Permissions;
import com.artipie.http.client.ClientSlices;
import com.artipie.settings.StorageAliases;
import com.artipie.settings.repo.proxy.ProxyConfig;
import com.artipie.settings.repo.proxy.YamlProxyConfig;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import org.reactivestreams.Publisher;

/**
 * Repository config.
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public final class RepoConfig {

    /**
     * HTTP client.
     */
    private final ClientSlices http;

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
     * Ctor.
     * @param http HTTP client
     * @param storages Repository storage aliases
     * @param prefix Storage prefix
     * @param yaml Config yaml
     */
    public RepoConfig(final ClientSlices http, final StorageAliases storages,
        final Key prefix, final YamlMapping yaml) {
        this.http = http;
        this.prefix = prefix;
        this.yaml = yaml;
        this.storages = storages;
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
        return this.storageOpt().map(MeasuredStorage::new).orElseThrow(
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
     * Create async yaml config from content publisher.
     * @param http HTTP client
     * @param storages Storage aliases
     * @param prefix Repository prefix
     * @param pub Yaml content publisher
     * @return Completion stage of yaml
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static CompletionStage<RepoConfig> fromPublisher(
        final ClientSlices http, final StorageAliases storages,
        final Key prefix, final Publisher<ByteBuffer> pub) {
        return new Concatenation(pub).single()
            .map(buf -> new Remaining(buf).bytes())
            .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
            .doOnSuccess(yaml -> Logger.debug(RepoConfig.class, "parsed yaml config:\n%s", yaml))
            .map(content -> Yaml.createYamlInput(content.toString()).readYamlMapping())
            .to(SingleInterop.get())
            .thenApply(yaml -> new RepoConfig(http, storages, prefix, yaml));
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

    /**
     * Get proxy config.
     *
     * @return Proxy config.
     */
    public ProxyConfig proxy() {
        return new YamlProxyConfig(this.http, this.storages, this.prefix, this.repoConfig());
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

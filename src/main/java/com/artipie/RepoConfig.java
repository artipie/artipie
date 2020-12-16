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
import com.artipie.asto.Concatenation;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.http.auth.Permissions;
import com.artipie.repo.ProxyConfig;
import com.artipie.repo.StorageYamlConfig;
import com.artipie.repo.YamlProxyConfig;
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
 */
@SuppressWarnings("PMD.TooManyMethods")
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
     * Ctor.
     * @param storages Repository storage aliases
     * @param prefix Storage prefix
     * @param yaml Config yaml
     */
    public RepoConfig(final StorageAliases storages, final Key prefix,
        final YamlMapping yaml) {
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
     * @param storages Storage aliases
     * @param prefix Repository prefix
     * @param pub Yaml content publisher
     * @return Completion stage of yaml
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    public static CompletionStage<RepoConfig> fromPublisher(final StorageAliases storages,
        final Key prefix, final Publisher<ByteBuffer> pub) {
        return new Concatenation(pub).single()
            .map(buf -> new Remaining(buf).bytes())
            .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
            .doOnSuccess(yaml -> Logger.debug(RepoConfig.class, "parsed yaml config:\n%s", yaml))
            .map(content -> Yaml.createYamlInput(content.toString()).readYamlMapping())
            .to(SingleInterop.get())
            .thenApply(yaml -> new RepoConfig(storages, prefix, yaml));
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
        return new YamlProxyConfig(this.storages, this.prefix, this.repoConfig());
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

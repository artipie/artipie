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
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Repository config.
 * @since 0.2
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.AvoidFieldNameMatchingMethodName"})
public final class RepoConfig {

    /**
     * The Vert.x instance.
     */
    private final Vertx vertx;

    /**
     * Source yaml future.
     */
    private final CompletionStage<YamlMapping> yaml;

    /**
     * Ctor.
     * @param vertx The Vert.x instance.
     * @param content Flow content
     */
    public RepoConfig(final Vertx vertx, final Publisher<ByteBuffer> content) {
        this.vertx = vertx;
        this.yaml = RepoConfig.yamlFromPublisher(content);
    }

    /**
     * Repository type.
     * @return Async string of type
     */
    public CompletionStage<String> type() {
        return this.string("type");
    }

    /**
     * Repository path.
     * @return Async string of path
     */
    public CompletionStage<String> path() {
        return this.string("path");
    }

    /**
     * Repository URL.
     *
     * @return Async string of URL
     */
    public CompletionStage<URL> url() {
        return this.string("url").thenApply(
            str -> {
                try {
                    return new URL(str);
                } catch (final MalformedURLException ex) {
                    throw new IllegalArgumentException(
                        String.format("Failed to build URL from '%s'", str),
                        ex
                    );
                }
            }
        );
    }

    /**
     * Read maximum allowed Content-Length value for incoming requests.
     *
     * @return Maximum allowed value, empty if none specified.
     */
    public CompletionStage<Optional<Long>> contentLengthMax() {
        return this.stringOpt("content-length-max").thenApply(opt -> opt.map(Long::valueOf));
    }

    /**
     * Storage.
     * @return Async storage for repo
     */
    public CompletionStage<Storage> storage() {
        return this.repo()
            .thenApply(map -> map.yamlMapping("storage"))
            .thenApply((YamlMapping mapping) -> new YamlStorageSettings(mapping, this.vertx))
            .thenApply(YamlStorageSettings::storage);
    }

    /**
     * Custom repository configuration.
     * @return Async custom repository config or Optional.empty
     */
    public CompletionStage<Optional<YamlMapping>> settings() {
        return this.repo().thenApply(
            map -> Optional.ofNullable(map.yamlMapping("settings"))
        );
    }

    /**
     * Get vertx instance.
     * @return Vertx instance
     */
    public Vertx vertx() {
        return this.vertx;
    }

    /**
     * Reads string by key from repo part of YAML.
     *
     * @param key String key.
     * @return String value.
     */
    private CompletionStage<String> string(final String key) {
        return this.stringOpt(key).thenApply(
            opt -> opt.orElseThrow(
                () -> new IllegalStateException(String.format("yaml repo.%s is absent", key))
            )
        );
    }

    /**
     * Reads string by key from repo part of YAML.
     *
     * @param key String key.
     * @return String value, empty if none present.
     */
    private CompletionStage<Optional<String>> stringOpt(final String key) {
        return this.repo().thenApply(map -> Optional.ofNullable(map.string(key)));
    }

    /**
     * Repo part of YAML.
     *
     * @return Async YAML mapping
     */
    private CompletionStage<YamlMapping> repo() {
        return this.yaml.thenApply(
            map -> Objects.requireNonNull(map.yamlMapping("repo"), "yaml repo is null")
        );
    }

    /**
     * Create async yaml config from content publisher.
     * @param pub Flow publisher
     * @return Completion stage of yaml
     */
    private static CompletionStage<YamlMapping> yamlFromPublisher(
        final Publisher<ByteBuffer> pub
    ) {
        return Flowable.fromPublisher(pub)
            .reduce(
                new StringBuilder(),
                (acc, buf) -> acc.append(
                    new String(new Remaining(buf).bytes(), StandardCharsets.UTF_8)
                )
            )
            .doOnSuccess(yaml -> Logger.debug(RepoConfig.class, "parsed yaml config: %s", yaml))
            .map(content -> Yaml.createYamlInput(content.toString()).readYamlMapping())
            .to(SingleInterop.get())
            .toCompletableFuture();
    }
}

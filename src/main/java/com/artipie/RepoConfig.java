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
import com.artipie.asto.fs.FileStorage;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Repository config.
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class RepoConfig {

    /**
     * Source yaml future.
     */
    private final CompletionStage<YamlMapping> yaml;

    /**
     * Ctor.
     * @param content Flow content
     */
    public RepoConfig(final Publisher<ByteBuffer> content) {
        this.yaml = RepoConfig.yamlFromPublisher(content);
    }

    /**
     * Repository type.
     * @return Async string of type
     */
    public CompletionStage<String> type() {
        return this.yaml.thenApply(
            map -> Objects.requireNonNull(
                Objects.requireNonNull(map.yamlMapping("repo"), "yaml repo is null")
                    .string("type"),
                "yaml repo.type is null"
            )
        );
    }

    /**
     * Storage.
     * @return Async storage for repo
     */
    public CompletionStage<Storage> storage() {
        return this.yaml.thenApply(map -> map.yamlMapping("repo").yamlMapping("storage"))
            .thenApply(RepoConfig::storageFromConfig);
    }

    /**
     * Create ASTO storage from yaml config.
     * @param cfg Storage config mapping
     * @return Storage instance
     */
    private static Storage storageFromConfig(final YamlMapping cfg) {
        if (!"fs".equals(cfg.string("type"))) {
            throw new IllegalStateException("We support only `fs` storage type for now");
        }
        final Path root = Paths.get(cfg.string("path"));
        Logger.info(RepoConfig.class, "using file storage at %s", root);
        return new FileStorage(root, Vertx.vertx().fileSystem());
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

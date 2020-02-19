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
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;

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
    public RepoConfig(final Flow.Publisher<ByteBuffer> content) {
        this.yaml = RepoConfig.yamlFromPublisher(content);
    }

    /**
     * Repository type.
     * @return Async string of type
     */
    public CompletionStage<String> type() {
        return this.yaml.thenApply(map -> map.yamlMapping("repo").string("type"));
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
        return new FileStorage(Paths.get(cfg.string("path")));
    }

    /**
     * Create async yaml config from content publisher.
     * @param pub Flow publisher
     * @return Completion stage of yaml
     */
    private static CompletionStage<YamlMapping> yamlFromPublisher(
        final Flow.Publisher<ByteBuffer> pub
    ) {
        final CompletableFuture<YamlMapping> future = new CompletableFuture<>();
        pub.subscribe(new ConfigSubscriber(future));
        return future;
    }

    /**
     * Config flow subscriber for yaml mapping completable future.
     * @since 0.2
     */
    private static final class ConfigSubscriber implements Flow.Subscriber<ByteBuffer> {

        /**
         * The result.
         */
        private final CompletableFuture<YamlMapping> future;

        /**
         * Temporary file channel.
         * <p>
         * Created on subscribe.
         * </p>
         */
        private FileChannel file;

        /**
         * Current subscription.
         * <p>
         * Initialized on subscribe.
         * </p>
         */
        private Subscription sub;

        /**
         * Temporary file path.
         * <p>
         * Created on subscribe.
         * </p>
         */
        private Path path;

        /**
         * Config flow subsciber for yaml mapping future.
         * @param future Completable future for yaml mapping
         */
        ConfigSubscriber(final CompletableFuture<YamlMapping> future) {
            this.future = future;
        }

        @Override
        public void onSubscribe(final Subscription subscription) {
            this.sub = Objects.requireNonNull(subscription, "subscription is null");
            try {
                this.path = Files.createTempFile("cfg_", ".yaml");
                this.file = FileChannel.open(this.path, StandardOpenOption.CREATE_NEW);
            } catch (final IOException err) {
                this.future.completeExceptionally(err);
                subscription.cancel();
            }
            this.sub.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(final ByteBuffer item) {
            if (this.future.isCancelled()) {
                this.sub.cancel();
                try {
                    this.file.close();
                } catch (final IOException err) {
                    Logger.warn(this, "onNext(): failed to close temp file: %[exception]s", err);
                }
                return;
            }
            while (item.hasRemaining()) {
                try {
                    this.file.write(item);
                } catch (final IOException err) {
                    this.future.completeExceptionally(err);
                    this.sub.cancel();
                }
            }
        }

        @Override
        public void onError(final Throwable err) {
            if (!this.future.isCancelled()) {
                this.future.completeExceptionally(err);
            }
            try {
                this.file.close();
            } catch (final IOException iex) {
                Logger.warn(this, "onError(): failed to close temp file: %[exception]s", iex);
            }
        }

        @Override
        public void onComplete() {
            try {
                if (!this.future.isCancelled()) {
                    this.future.complete(
                        Yaml.createYamlInput(this.path.toFile()).readYamlMapping()
                    );
                }
            } catch (final IOException err) {
                this.future.completeExceptionally(err);
            } finally {
                try {
                    this.file.close();
                } catch (final IOException err) {
                    Logger.warn(
                        this,
                        "onComplete(): failed to close temp file: %[exception]s",
                        err
                    );
                }
            }
        }
    }
}

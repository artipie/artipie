/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem.ruby;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jruby.Ruby;
import org.jruby.javasupport.JavaEmbedUtils;

/**
 * Share ruby runtime and interpreter.
 * @since 1.0
 */
public final class SharedRuntime {

    /**
     * Ruby runtime factory.
     */
    private final Supplier<Ruby> factory;

    /**
     * Synchronization lock.
     */
    private final Object lock;

    /**
     * Loaded plugins by ID.
     */
    private final ConcurrentMap<String, Boolean> plugins;

    /**
     * Runtime cache.
     */
    private volatile Ruby runtime;

    /**
     * New default shared ruby runtime.
     */
    public SharedRuntime() {
        this(
            () -> JavaEmbedUtils.initialize(Collections.emptyList())
        );
    }

    /**
     * New shared ruby runtime with specified factory.
     * @param factory Runtime factory
     */
    public SharedRuntime(final Supplier<Ruby> factory) {
        this.factory = factory;
        this.lock = new Object();
        this.plugins = new ConcurrentHashMap<>();
    }

    /**
     * Apply shared runtime and interpreted to function async.
     * @param applier Function to apply
     * @param <T> Apply function result type
     * @return Future with result of the function
     */
    public <T extends RubyPlugin> CompletionStage<T> apply(final Function<Ruby, T> applier) {
        return CompletableFuture.supplyAsync(
            () -> {
                if (this.runtime == null) {
                    synchronized (this.lock) {
                        if (this.runtime == null) {
                            this.runtime = this.factory.get();
                        }
                    }
                }
                return applier.apply(this.runtime);
            }
        ).thenApply(
            plugin -> {
                this.plugins.computeIfAbsent(
                    plugin.identifier(), id -> {
                        plugin.initialize();
                        return Boolean.TRUE;
                    }
                );
                return plugin;
            }
        );
    }

    /**
     * Ruby plugin. Could be loaded and initialized only once.
     * @since 1.0
     */
    public interface RubyPlugin {

        /**
         * Plugin unique identifier.
         * @return ID string
         */
        String identifier();

        /**
         * Initialize once.
         */
        void initialize();
    }
}

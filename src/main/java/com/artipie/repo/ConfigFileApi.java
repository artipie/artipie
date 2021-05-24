/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.repo;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.management.ConfigFiles;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Wrapper for {@link ConfigFile} for `management-api` module. It allows
 * to use the logic of working with two extensions in another module.
 * @since 0.14
 */
public final class ConfigFileApi implements ConfigFiles {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Storage
     */
    public ConfigFileApi(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletionStage<Boolean> exists(final Key filename) {
        return new ConfigFile(filename).existsIn(this.storage);
    }

    @Override
    public CompletionStage<Content> value(final Key filename) {
        return new ConfigFile(filename).valueFrom(this.storage);
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        return this.storage.save(key, content);
    }

    @Override
    public String name(final Key filename) {
        return new ConfigFile(filename).name();
    }

    @Override
    public Optional<String> extension(final Key filename) {
        return new ConfigFile(filename).extension();
    }

    @Override
    public boolean isYamlOrYml(final Key filename) {
        return new ConfigFile(filename).isYamlOrYml();
    }

}

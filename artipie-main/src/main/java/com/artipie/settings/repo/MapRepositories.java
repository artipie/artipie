/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings.repo;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.settings.AliasSettings;
import com.artipie.settings.ConfigFile;
import com.artipie.settings.Settings;
import com.artipie.settings.StorageByAlias;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MapRepositories implements Repositories {

    private final static Logger LOGGER = LoggerFactory.getLogger(MapRepositories.class);

    private final Settings settings;

    private final Map<String, RepoConfig> map;

    public MapRepositories(final Settings settings) {
        this.settings = settings;
        this.map = new ConcurrentHashMap<>();
        refresh();
    }

    @Override
    public Optional<RepoConfig> config(final String name) {
        return Optional.ofNullable(this.map.get(new ConfigFile(name).name()));
    }

    @Override
    public Collection<RepoConfig> configs() {
        return Collections.unmodifiableCollection(this.map.values());
    }

    @Override
    public void refresh() {
        this.map.clear();
        final Collection<Key> keys = settings.repoConfigsStorage()
            .list(Key.ROOT).
            toCompletableFuture().join();
        for (Key key : keys) {
            final ConfigFile file = new ConfigFile(key);
            if (!file.isSystem() && file.isYamlOrYml()) {
                final Storage storage = this.settings.repoConfigsStorage();
                final CompletableFuture<StorageByAlias> alias = new AliasSettings(storage)
                    .find(key);
                final String content = file.valueFrom(storage)
                    .toCompletableFuture().join().asString();
                try {
                    this.map.put(file.name(), RepoConfig.from(
                        Yaml.createYamlInput(content).readYamlMapping(),
                        alias.join(), new Key.From(file.name()),
                        this.settings.caches().storagesCache(),
                        this.settings.metrics().storage()
                    ));
                } catch (Exception e) {
                    LOGGER.error("Can't parse the repository config file: " + file.name(), e);
                }
            }
        }
    }
}

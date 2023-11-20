/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.security.policy;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StoragesLoader;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Policy factory to create {@link CachedYamlPolicy}. Yaml policy is read from storage,
 * and it's required to describe this storage in the configuration.
 * Configuration format is the following:
 *
 * policy:
 *   type: artipie
 *   eviction_millis: 60000 # not required, default 3 min
 *   storage:
 *     type: fs
 *     path: /some/path
 *
 * The storage itself is expected to have yaml files with permissions in the following structure:
 *
 * ..
 * ├── roles
 * │   ├── java-dev.yaml
 * │   ├── admin.yaml
 * │   ├── ...
 * ├── users
 * │   ├── david.yaml
 * │   ├── jane.yaml
 * │   ├── ...
 *
 * @since 1.2
 */
@ArtipiePolicyFactory("artipie")
public final class YamlPolicyFactory implements PolicyFactory {

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public Policy<?> getPolicy(final Config config) {
        final Config sub = config.config("storage");
        long eviction;
        try {
            eviction = Long.parseLong(config.string("eviction_millis"));
        // @checkstyle IllegalCatchCheck (5 lines)
        } catch (final Exception err) {
            // @checkstyle MagicNumberCheck (2 lines)
            eviction = 180_000L;
        }
        try {
            return new CachedYamlPolicy(
                new BlockingStorage(
                    new StoragesLoader().newObject(
                        sub.string("type"),
                        new Config.YamlStorageConfig(
                            Yaml.createYamlInput(sub.toString()).readYamlMapping()
                        )
                    )
                ),
                eviction
            );
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.redis;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.ArtipieStorageFactory;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StorageFactory;
import java.io.IOException;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;

/**
 * Redis storage factory.
 *
 * @since 0.1
 */
@ArtipieStorageFactory("redis")
public final class RedisStorageFactory implements StorageFactory {
    /**
     * Default redis object name.
     */
    public static final String DEF_OBJ_NAME = "artipie-redis";

    @Override
    public Storage newStorage(final Config cfg) {
        try {
            String name = cfg.string("name");
            if (name == null) {
                name = RedisStorageFactory.DEF_OBJ_NAME;
            }
            final RedissonClient redisson = Redisson.create(
                org.redisson.config.Config.fromYAML(
                    new Config.StrictStorageConfig(cfg)
                        .string("config")
                )
            );
            return new RedisStorage(redisson.getMap(name), redisson.getId());
        } catch (final IOException err) {
            throw new ArtipieIOException(err);
        }
    }
}

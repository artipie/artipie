/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.fs;

import com.artipie.asto.Storage;
import com.artipie.asto.factory.ArtipieStorageFactory;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StorageFactory;
import io.vertx.reactivex.core.Vertx;
import java.nio.file.Paths;

/**
 * File storage factory.
 *
 * @since 0.1
 */
@ArtipieStorageFactory("vertx-file")
public final class VertxFileStorageFactory implements StorageFactory {
    @Override
    public Storage newStorage(final Config cfg) {
        return new VertxFileStorage(
            Paths.get(new Config.StrictStorageConfig(cfg).string("path")),
            Vertx.vertx()
        );
    }
}

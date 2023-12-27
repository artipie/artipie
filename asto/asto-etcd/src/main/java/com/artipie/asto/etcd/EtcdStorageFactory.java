/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.etcd;

import com.artipie.asto.Storage;
import com.artipie.asto.factory.ArtipieStorageFactory;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StorageFactory;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.ClientBuilder;
import java.time.Duration;
import java.util.Arrays;

/**
 * Etcd storage factory.
 * @since 0.1
 */
@ArtipieStorageFactory("etcd")
public final class EtcdStorageFactory implements StorageFactory {
    @Override
    public Storage newStorage(final Config cfg) {
        final Config connection = new Config.StrictStorageConfig(cfg)
            .config("connection");
        final String[] endpoints = connection.sequence("endpoints").toArray(new String[0]);
        final ClientBuilder builder = Client.builder().endpoints(endpoints);
        final String sto = connection.string("timeout");
        if (sto != null) {
            builder.connectTimeout(Duration.ofMillis(Integer.parseInt(sto)));
        }
        return new EtcdStorage(builder.build(), Arrays.toString(endpoints));
    }
}

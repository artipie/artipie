/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.api.ssl;

import com.artipie.asto.Storage;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;

/**
 * Key store.
 * @since 0.26
 */
public interface KeyStore {
    /**
     * Checks if SSL is enabled.
     * @return True is SSL enabled.
     */
    boolean enabled();

    /**
     * Checks if configuration for this type of KeyStore is present.
     * @return True if it is configured.
     */
    boolean isConfigured();

    /**
     * Provides SSL-options for http server.
     * @param vertx Vertx.
     * @param storage Artipie settings storage.
     * @return HttpServer
     */
    HttpServerOptions secureOptions(Vertx vertx, Storage storage);
}

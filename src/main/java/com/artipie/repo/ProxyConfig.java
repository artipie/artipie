/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.repo;

import com.artipie.asto.Storage;
import com.artipie.http.client.auth.Authenticator;
import java.util.Collection;
import java.util.Optional;

/**
 * Proxy repository config.
 *
 * @since 0.12
 */
public interface ProxyConfig {

    /**
     * Get all configured remote endpoints.
     *
     * @return Remote endpoints.
     */
    Collection<? extends Remote> remotes();

    /**
     * Proxy repository remote.
     *
     * @since 0.12
     */
    interface Remote {

        /**
         * Get URL.
         *
         * @return URL.
         */
        String url();

        /**
         * Get authenticator.
         *
         * @return Authenticator.
         */
        Authenticator auth();

        /**
         * Get cache storage.
         *
         * @return Cache storage, empty if not configured.
         */
        Optional<Storage> cache();
    }
}

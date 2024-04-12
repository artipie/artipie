/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http.proxy;

import com.artipie.asto.cache.Cache;
import com.artipie.composer.Repository;
import com.artipie.composer.http.PackageMetadataSlice;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.UriClientSlice;
import com.artipie.http.client.auth.AuthClientSlice;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.rt.MethodRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceSimple;

import java.net.URI;

/**
 * Composer proxy repository slice.
 * @since 0.4
 */
public class ComposerProxySlice extends Slice.Wrap {
    /**
     * New Composer proxy without cache and without authentication.
     * @param clients HTTP clients
     * @param remote Remote URI
     * @param repo Repository
     */
    public ComposerProxySlice(final ClientSlices clients, final URI remote, final Repository repo) {
        this(clients, remote, repo, Authenticator.ANONYMOUS, Cache.NOP);
    }

    /**
     * New Composer proxy without cache.
     * @param clients HTTP clients
     * @param remote Remote URI
     * @param repo Repository
     * @param auth Authenticator
     */
    public ComposerProxySlice(
        final ClientSlices clients, final URI remote,
        final Repository repo, final Authenticator auth
    ) {
        this(clients, remote, repo, auth, Cache.NOP);
    }

    /**
     * New Composer proxy slice with cache.
     * @param clients HTTP clients
     * @param remote Remote URI
     * @param repository Repository
     * @param auth Authenticator
     * @param cache Repository cache
     */
    public ComposerProxySlice(
        final ClientSlices clients,
        final URI remote,
        final Repository repository,
        final Authenticator auth,
        final Cache cache
    ) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(PackageMetadataSlice.ALL_PACKAGES),
                        MethodRule.GET
                    ),
                    new SliceSimple(
                        ResponseBuilder.ok()
                            .jsonBody("{\"packages\":{}, \"metadata-url\":\"/p2/%package%.json\"}")
                            .build()
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(PackageMetadataSlice.PACKAGE),
                        MethodRule.GET
                    ),
                    new CachedProxySlice(remote(clients, remote, auth), repository, cache)
                ),
                new RtRulePath(
                    RtRule.FALLBACK,
                    new SliceSimple(ResponseBuilder.methodNotAllowed().build())
                )
            )
        );
    }

    /**
     * Build client slice for target URI.
     * @param client Client slices
     * @param remote Remote URI
     * @param auth Authenticator
     * @return Client slice for target URI.
     */
    private static Slice remote(
        final ClientSlices client,
        final URI remote,
        final Authenticator auth
    ) {
        return new AuthClientSlice(new UriClientSlice(client, remote), auth);
    }
}

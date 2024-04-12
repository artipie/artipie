/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi.http;

import com.artipie.asto.Storage;
import com.artipie.asto.cache.FromStorageCache;
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
import com.artipie.scheduling.ProxyArtifactEvent;

import java.net.URI;
import java.util.Optional;
import java.util.Queue;

/**
 * Python proxy slice.
 * @since 0.7
 */
public final class PyProxySlice extends Slice.Wrap {

    /**
     * New maven proxy without cache.
     * @param clients HTTP clients
     * @param remote Remote URI
     * @param storage Cache storage
     */
    public PyProxySlice(final ClientSlices clients, final URI remote, final Storage storage) {
        this(clients, remote, Authenticator.ANONYMOUS, storage, Optional.empty(), "*");
    }

    /**
     * Ctor.
     * @param clients Http clients
     * @param remote Remote URI
     * @param auth Authenticator
     * @param cache Repository cache storage
     * @param events Artifact events queue
     * @param rname Repository name
     */
    @SuppressWarnings("PMD.UnusedFormalParameter")
    public PyProxySlice(
        final ClientSlices clients,
        final URI remote,
        final Authenticator auth,
        final Storage cache,
        final Optional<Queue<ProxyArtifactEvent>> events,
        final String rname
    ) {
        super(
            new SliceRoute(
                new RtRulePath(
                    MethodRule.GET,
                    new ProxySlice(
                        new AuthClientSlice(new UriClientSlice(clients, remote), auth),
                        new FromStorageCache(cache), events, rname
                    )
                ),
                new RtRulePath(
                    RtRule.FALLBACK,
                    new SliceSimple(ResponseBuilder.methodNotAllowed().build())
                )
            )
        );
    }

}

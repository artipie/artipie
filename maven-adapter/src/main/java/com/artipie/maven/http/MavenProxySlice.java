/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.cache.Cache;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.UriClientSlice;
import com.artipie.http.client.auth.AuthClientSlice;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceSimple;
import com.artipie.scheduling.ProxyArtifactEvent;

import java.net.URI;
import java.util.Optional;
import java.util.Queue;

/**
 * Maven proxy repository slice.
 * @since 0.5
 */
public final class MavenProxySlice extends Slice.Wrap {

    /**
     * New maven proxy without cache.
     * @param clients HTTP clients
     * @param remote Remote URI
     * @param auth Authenticator
     * @param cache Cache implementation
     */
    public MavenProxySlice(final ClientSlices clients, final URI remote,
        final Authenticator auth, final Cache cache) {
        this(clients, remote, auth, cache, Optional.empty(), "*");
    }

    /**
     * Ctor for tests.
     * @param client Http client
     * @param uri Origin URI
     * @param authenticator Auth
     */
    MavenProxySlice(
        final JettyClientSlices client, final URI uri,
        final Authenticator authenticator
    ) {
        this(client, uri, authenticator, Cache.NOP, Optional.empty(), "*");
    }

    /**
     * New Maven proxy slice with cache.
     * @param clients HTTP clients
     * @param remote Remote URI
     * @param auth Authenticator
     * @param cache Repository cache
     * @param events Artifact events queue
     * @param rname Repository name
     */
    public MavenProxySlice(
        final ClientSlices clients,
        final URI remote,
        final Authenticator auth,
        final Cache cache,
        final Optional<Queue<ProxyArtifactEvent>> events,
        final String rname
    ) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new ByMethodsRule(RqMethod.HEAD),
                    new HeadProxySlice(remote(clients, remote, auth))
                ),
                new RtRulePath(
                    new ByMethodsRule(RqMethod.GET),
                    new CachedProxySlice(remote(clients, remote, auth), cache, events, rname)
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
     *
     * @param client Client slices.
     * @param remote Remote URI.
     * @param auth Authenticator.
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

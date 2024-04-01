/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.adapters.docker;

import com.artipie.asto.Content;
import com.artipie.asto.SubStorage;
import com.artipie.docker.Docker;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.asto.RegistryRoot;
import com.artipie.docker.cache.CacheDocker;
import com.artipie.docker.composite.MultiReadDocker;
import com.artipie.docker.composite.ReadWriteDocker;
import com.artipie.docker.http.DockerSlice;
import com.artipie.docker.http.TrimmedDocker;
import com.artipie.docker.proxy.ProxyDocker;
import com.artipie.http.DockerRoutingSlice;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.RemoteConfig;
import com.artipie.http.client.auth.AuthClientSlice;
import com.artipie.http.rq.RequestLine;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.policy.Policy;
import com.artipie.settings.repo.RepoConfig;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Docker proxy slice created from config.
 *
 * @since 0.9
 */
public final class DockerProxy implements Slice {

    private final Slice delegate;

    /**
     * Ctor.
     *
     * @param client HTTP client.
     * @param cfg Repository configuration.
     * @param policy Access policy.
     * @param auth Authentication mechanism.
     * @param events Artifact events queue
     */
    public DockerProxy(
        final ClientSlices client,
        final RepoConfig cfg,
        final Policy<?> policy,
        final Authentication auth,
        final Optional<Queue<ArtifactEvent>> events
    ) {
        this.delegate = createProxy(client, cfg, policy, auth, events);
    }

    @Override
    public CompletableFuture<Response> response(
        final RequestLine line,
        final Headers headers,
        final Content body
    ) {
        return this.delegate.response(line, headers, body);
    }

    /**
     * Creates Docker proxy repository slice from configuration.
     *
     * @return Docker proxy slice.
     */
    private static Slice createProxy(
            final ClientSlices client,
            final RepoConfig cfg,
            final Policy<?> policy,
            final Authentication auth,
            final Optional<Queue<ArtifactEvent>> events
    ) {
        final Docker proxies = new MultiReadDocker(
            cfg.remotes().stream().map(r -> proxy(client, cfg, events, r))
                .collect(Collectors.toList())
        );
        Docker docker = cfg.storageOpt()
            .<Docker>map(
                storage -> {
                    final AstoDocker local = new AstoDocker(
                        new SubStorage(RegistryRoot.V2, storage)
                    );
                    return new ReadWriteDocker(new MultiReadDocker(local, proxies), local);
                }
            )
            .orElse(proxies);
        docker = new TrimmedDocker(docker, cfg.name());
        Slice slice = new DockerSlice(
            docker,
            policy,
            new BasicAuthScheme(auth),
            events, cfg.name()
        );
        if (cfg.port().isEmpty()) {
            slice = new DockerRoutingSlice.Reverted(slice);
        }
        return slice;
    }

    /**
     * Create proxy from YAML config.
     *
     * @param remote YAML remote config.
     * @return Docker proxy.
     */
    private static Docker proxy(
            final ClientSlices client,
            final RepoConfig cfg,
            final Optional<Queue<ArtifactEvent>> events,
            final RemoteConfig remote
    ) {
        final Docker proxy = new ProxyDocker(
            AuthClientSlice.withClientSlice(client, remote)
        );
        return cfg.storageOpt().<Docker>map(
            cache -> new CacheDocker(
                proxy,
                new AstoDocker(new SubStorage(RegistryRoot.V2, cache)),
                events,
                cfg.name()
            )
        ).orElse(proxy);
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.adapters.docker;

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
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.auth.AuthClientSlice;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.policy.Policy;
import com.artipie.settings.repo.RepoConfig;
import com.artipie.settings.repo.proxy.ProxyConfig;
import com.artipie.settings.repo.proxy.YamlProxyConfig;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;

/**
 * Docker proxy slice created from config.
 *
 * @since 0.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class DockerProxy implements Slice {

    /**
     * HTTP client.
     */
    private final ClientSlices client;

    /**
     * Repository configuration.
     */
    private final RepoConfig cfg;

    /**
     * Standalone flag.
     */
    private final boolean standalone;

    /**
     * Access policy.
     */
    private final Policy<?> policy;

    /**
     * Authentication mechanism.
     */
    private final Authentication auth;

    /**
     * Artifact events queue.
     */
    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * Ctor.
     *
     * @param client HTTP client.
     * @param standalone Standalone flag.
     * @param cfg Repository configuration.
     * @param policy Access policy.
     * @param auth Authentication mechanism.
     * @param events Artifact events queue
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    public DockerProxy(
        final ClientSlices client,
        final boolean standalone,
        final RepoConfig cfg,
        final Policy<?> policy,
        final Authentication auth,
        final Optional<Queue<ArtifactEvent>> events
    ) {
        this.client = client;
        this.cfg = cfg;
        this.standalone = standalone;
        this.policy = policy;
        this.auth = auth;
        this.events = events;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return this.delegate().response(line, headers, body);
    }

    /**
     * Creates Docker proxy repository slice from configuration.
     *
     * @return Docker proxy slice.
     */
    private Slice delegate() {
        final Docker proxies = new MultiReadDocker(
            new YamlProxyConfig(this.cfg)
                .remotes().stream().map(this::proxy)
                .collect(Collectors.toList())
        );
        Docker docker = this.cfg.storageOpt()
            .<Docker>map(
                storage -> {
                    final AstoDocker local = new AstoDocker(
                        new SubStorage(RegistryRoot.V2, storage)
                    );
                    return new ReadWriteDocker(new MultiReadDocker(local, proxies), local);
                }
            )
            .orElse(proxies);
        docker = new TrimmedDocker(docker, this.cfg.name());
        Slice slice = new DockerSlice(
            docker,
            this.policy,
            new BasicAuthScheme(this.auth),
            this.events, this.cfg.name()
        );
        if (!this.standalone) {
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
    private Docker proxy(final ProxyConfig.Remote remote) {
        final Docker proxy = new ProxyDocker(
            new AuthClientSlice(this.client.https(remote.url()), remote.auth(this.client))
        );
        return this.cfg.storageOpt().<Docker>map(
            cache -> new CacheDocker(
                proxy,
                new AstoDocker(new SubStorage(RegistryRoot.V2, cache)),
                this.events, this.cfg.name()
            )
        ).orElse(proxy);
    }
}

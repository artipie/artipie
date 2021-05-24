/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker;

import com.artipie.RepoConfig;
import com.artipie.asto.SubStorage;
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
import com.artipie.http.auth.Permissions;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.auth.AuthClientSlice;
import com.artipie.repo.ProxyConfig;
import java.nio.ByteBuffer;
import java.util.Map;
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
     * Access permissions.
     */
    private final Permissions perms;

    /**
     * Authentication mechanism.
     */
    private final Authentication auth;

    /**
     * Ctor.
     *
     * @param client HTTP client.
     * @param standalone Standalone flag.
     * @param cfg Repository configuration.
     * @param perms Access permissions.
     * @param auth Authentication mechanism.
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    public DockerProxy(
        final ClientSlices client,
        final boolean standalone,
        final RepoConfig cfg,
        final Permissions perms,
        final Authentication auth) {
        this.client = client;
        this.cfg = cfg;
        this.standalone = standalone;
        this.perms = perms;
        this.auth = auth;
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
            this.cfg.proxy().remotes().stream().map(
                remote -> proxy(this.client, remote)
            ).collect(Collectors.toList())
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
        if (!this.standalone) {
            docker = new TrimmedDocker(docker, this.cfg.name());
        }
        Slice slice = new DockerSlice(
            docker,
            new DockerPermissions(this.perms),
            new BasicAuthScheme(this.auth)
        );
        if (!this.standalone) {
            slice = new DockerRoutingSlice.Reverted(slice);
        }
        return slice;
    }

    /**
     * Create proxy from YAML config.
     *
     * @param slices HTTP client slices.
     * @param remote YAML remote config.
     * @return Docker proxy.
     */
    private static Docker proxy(final ClientSlices slices, final ProxyConfig.Remote remote) {
        final Docker proxy = new ProxyDocker(
            new AuthClientSlice(slices.https(remote.url()), remote.auth())
        );
        return remote.cache().<Docker>map(
            storage -> new CacheDocker(
                proxy,
                new AstoDocker(new SubStorage(RegistryRoot.V2, storage))
            )
        ).orElse(proxy);
    }
}

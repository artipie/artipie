/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie;

import com.artipie.adapters.docker.DockerProxy;
import com.artipie.adapters.file.FileProxy;
import com.artipie.adapters.maven.MavenProxy;
import com.artipie.adapters.php.ComposerProxy;
import com.artipie.adapters.pypi.PypiProxy;
import com.artipie.asto.Key;
import com.artipie.asto.SubStorage;
import com.artipie.auth.LoggingAuth;
import com.artipie.composer.AstoRepository;
import com.artipie.composer.http.PhpComposer;
import com.artipie.conan.ItemTokenizer;
import com.artipie.conan.http.ConanSlice;
import com.artipie.conda.http.CondaSlice;
import com.artipie.debian.Config;
import com.artipie.debian.http.DebianSlice;
import com.artipie.docker.Docker;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.asto.RegistryRoot;
import com.artipie.docker.http.DockerSlice;
import com.artipie.docker.http.TrimmedDocker;
import com.artipie.files.FilesSlice;
import com.artipie.gem.http.GemSlice;
import com.artipie.helm.http.HelmSlice;
import com.artipie.hex.http.HexSlice;
import com.artipie.http.ContentLengthRestriction;
import com.artipie.http.ContinueSlice;
import com.artipie.http.DockerRoutingSlice;
import com.artipie.http.GoSlice;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.auth.Tokens;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.filter.FilterSlice;
import com.artipie.http.filter.Filters;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.SliceSimple;
import com.artipie.http.slice.TrimPathSlice;
import com.artipie.maven.http.MavenSlice;
import com.artipie.npm.http.NpmSlice;
import com.artipie.npm.proxy.NpmProxy;
import com.artipie.npm.proxy.http.NpmProxySlice;
import com.artipie.nuget.http.NuGet;
import com.artipie.pypi.http.PySlice;
import com.artipie.rpm.http.RpmSlice;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.scheduling.MetadataEventQueues;
import com.artipie.security.policy.Policy;
import com.artipie.settings.Settings;
import com.artipie.settings.repo.RepoConfig;
import com.artipie.settings.repo.Repositories;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import io.vertx.core.Vertx;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class RepositorySlices {

    /**
     * Pattern to trim path before passing it to adapters' slice.
     */
    private static final Pattern PATTERN = Pattern.compile("/(?:[^/.]+)(/.*)?");

    /**
     * Artipie settings.
     */
    private final Settings settings;

    private final Repositories repos;

    /**
     * Tokens: authentication and generation.
     */
    private final Tokens tokens;

    /**
     * Slice's cache.
     */
    private final LoadingCache<SliceKey, SliceValue> slices;

    /**
     * @param settings Artipie settings
     * @param repos Repositories
     * @param tokens Tokens: authentication and generation
     */
    public RepositorySlices(
        final Settings settings,
        final Repositories repos,
        final Tokens tokens
    ) {
        this.settings = settings;
        this.repos = repos;
        this.tokens = tokens;
        this.slices = CacheBuilder.newBuilder()
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .removalListener(
                (RemovalListener<SliceKey, SliceValue>) notification -> notification.getValue()
                    .client()
                    .ifPresent(JettyClientSlices::stop)
            )
            .build(
                new CacheLoader<>() {
                    @Override
                    public SliceValue load(final SliceKey key) {
                        return resolve(key.name(), key.port());
                    }
                }
            );
    }

    public Slice slice(final Key name, final int port) {
        return this.slices.getUnchecked(new SliceKey(name, port)).slice();
    }

    /**
     * Resolve {@link Slice} by provided configuration.
     *
     * @param name Repository name
     * @param port Repository port
     * @return Slice for repo
     */
    private SliceValue resolve(final Key name, final int port) {
        final Optional<RepoConfig> opt = repos.config(name.string());
        if (opt.isPresent()) {
            final RepoConfig cfg = opt.get();
            if (cfg.port().isEmpty() || cfg.port().getAsInt() == port) {
                return sliceFromConfig(cfg);
            }
        }
        return new SliceValue(new SliceSimple(new RsRepoNotFound(name)), Optional.empty());
    }

    private Optional<Queue<ArtifactEvent>> artifactEvents() {
        return this.settings.artifactMetadata()
            .map(MetadataEventQueues::eventQueue);
    }

    private SliceValue sliceFromConfig(final RepoConfig cfg) {
        final Slice slice;
        JettyClientSlices clientSlices = null;
        switch (cfg.type()) {
            case "file":
                slice = trimPathSlice(
                    new FilesSlice(
                        cfg.storage(),
                        securityPolicy(),
                        authentication(),
                        cfg.name(),
                        artifactEvents()
                    )
                );
                break;
            case "file-proxy":
                clientSlices = jettyClientSlices(cfg);
                slice = trimPathSlice(
                    new FileProxy(clientSlices, cfg, artifactEvents())
                );
                break;
            case "npm":
                slice = trimPathSlice(
                    new NpmSlice(
                        cfg.url(), cfg.storage(), securityPolicy(), tokens.auth(), cfg.name(), artifactEvents()
                    )
                );
                break;
            case "gem":
                slice = trimPathSlice(new GemSlice(cfg.storage()));
                break;
            case "helm":
                slice = trimPathSlice(
                    new HelmSlice(
                        cfg.storage(), cfg.url().toString(), securityPolicy(), authentication(), cfg.name(), artifactEvents()
                    )
                );
                break;
            case "rpm":
                slice = trimPathSlice(
                    new RpmSlice(cfg.storage(), securityPolicy(), authentication(),
                        new com.artipie.rpm.RepoConfig.FromYaml(cfg.settings(), cfg.name()))
                );
                break;
            case "php":
                slice = trimPathSlice(
                    new PhpComposer(
                        new AstoRepository(cfg.storage(), Optional.of(cfg.url().toString())),
                        securityPolicy(), authentication(), cfg.name(), artifactEvents()
                    )
                );
                break;
            case "php-proxy":
                clientSlices = jettyClientSlices(cfg);
                slice = trimPathSlice(new ComposerProxy(clientSlices, cfg));
                break;
            case "nuget":
                slice = trimPathSlice(
                    new NuGet(
                        cfg.url(), new com.artipie.nuget.AstoRepository(cfg.storage()),
                        securityPolicy(), authentication(), cfg.name(), artifactEvents()
                    )
                );
                break;
            case "maven":
                slice = trimPathSlice(
                    new MavenSlice(cfg.storage(), securityPolicy(),
                        authentication(), cfg.name(), artifactEvents())
                );
                break;
            case "maven-proxy":
                clientSlices = jettyClientSlices(cfg);
                slice = trimPathSlice(
                    new MavenProxy(clientSlices, cfg,
                        settings.artifactMetadata().flatMap(queues -> queues.proxyEventQueues(cfg)))
                );
                break;
            case "go":
                slice = trimPathSlice(
                    new GoSlice(cfg.storage(), securityPolicy(), authentication(), cfg.name())
                );
                break;
            case "npm-proxy":
                clientSlices = jettyClientSlices(cfg);
                slice = new NpmProxySlice(
                    cfg.path(),
                    new NpmProxy(
                        URI.create(
                            cfg.settings().orElseThrow().yamlMapping("remote").string("url")
                        ), cfg.storage(), clientSlices),
                    settings.artifactMetadata().flatMap(queues -> queues.proxyEventQueues(cfg)
                    )
                );
                break;
            case "pypi":
                slice = trimPathSlice(
                    new PySlice(cfg.storage(), securityPolicy(), authentication(), cfg.name(), artifactEvents())
                );
                break;
            case "pypi-proxy":
                clientSlices = jettyClientSlices(cfg);
                slice = trimPathSlice(
                    new PypiProxy(
                        clientSlices, cfg,
                        settings.artifactMetadata()
                            .flatMap(queues -> queues.proxyEventQueues(cfg))
                    )
                );
                break;
            case "docker":
                final Docker docker = new AstoDocker(
                    new SubStorage(RegistryRoot.V2, cfg.storage())
                );
                if (cfg.port().isPresent()) {
                    slice = new DockerSlice(docker, securityPolicy(),
                        new BasicAuthScheme(authentication()), artifactEvents(), cfg.name());
                } else {
                    slice = new DockerRoutingSlice.Reverted(
                        new DockerSlice(new TrimmedDocker(docker, cfg.name()),
                            securityPolicy(), new BasicAuthScheme(authentication()),
                            artifactEvents(), cfg.name())
                    );
                }
                break;
            case "docker-proxy":
                clientSlices = jettyClientSlices(cfg);
                slice = new DockerProxy(clientSlices, cfg.port().isPresent(),
                    cfg, securityPolicy(), authentication(), artifactEvents());
                break;
            case "deb":
                slice = trimPathSlice(
                    new DebianSlice(
                        cfg.storage(), securityPolicy(), authentication(),
                        new Config.FromYaml(cfg.name(), cfg.settings(), settings.configStorage()),
                        artifactEvents()
                    )
                );
                break;
            case "conda":
                slice = new CondaSlice(
                    cfg.storage(), securityPolicy(), authentication(),
                    tokens, cfg.url().toString(), cfg.name(), artifactEvents()
                );
                break;
            case "conan":
                slice = new ConanSlice(
                    cfg.storage(), securityPolicy(), authentication(), tokens,
                    new ItemTokenizer(Vertx.vertx()), cfg.name()
                );
                break;
            case "hexpm":
                slice = trimPathSlice(
                    new HexSlice(cfg.storage(), securityPolicy(), authentication(),
                        artifactEvents(), cfg.name())
                );
                break;
            default:
                throw new IllegalStateException(
                    String.format("Unsupported repository type '%s", cfg.type())
                );
        }
        return new SliceValue(
            wrapIntoCommonSlices(slice, cfg),
            Optional.ofNullable(clientSlices)
        );
    }

    private Slice wrapIntoCommonSlices(
        final Slice origin,
        final RepoConfig cfg
    ) {
        Optional<Filters> opt = settings.caches()
            .filtersCache()
            .filters(cfg.name(), cfg.repoYaml());
        Slice res = opt.isPresent() ? new FilterSlice(origin, opt.get()) : origin;
        return new ContinueSlice(
            cfg.contentLengthMax()
                .<Slice>map(limit -> new ContentLengthRestriction(res, limit))
                .orElse(res)
        );
    }

    private Authentication authentication() {
        return new LoggingAuth(settings.authz().authentication());
    }

    private Policy<?> securityPolicy() {
        return this.settings.authz().policy();
    }

    private JettyClientSlices jettyClientSlices(final RepoConfig cfg) {
        JettyClientSlices res = new JettyClientSlices(
            cfg.httpClientSettings().orElseGet(settings::httpClientSettings)
        );
        res.start();
        return res;
    }

    private static Slice trimPathSlice(final Slice original) {
        return new TrimPathSlice(original, RepositorySlices.PATTERN);
    }

    /**
     * Slice's cache key.
     */
    record SliceKey(Key name, int port) {
    }

    /**
     * Slice's cache value.
     */
    record SliceValue(Slice slice, Optional<JettyClientSlices> client) {
    }

    /**
     * Repo not found response.
     */
    private static final class RsRepoNotFound extends Response.Wrap {

        /**
         * New repo not found response.
         *
         * @param repo Repo name
         */
        RsRepoNotFound(final Key repo) {
            super(
                new RsWithBody(
                    StandardRs.NOT_FOUND,
                    String.format("Repository '%s' not found", repo.string()),
                    StandardCharsets.UTF_8
                )
            );
        }
    }
}

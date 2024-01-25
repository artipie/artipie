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
import com.artipie.http.Slice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.auth.Tokens;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.filter.FilterSlice;
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
import io.vertx.core.Vertx;
import java.net.URI;
import java.util.Optional;
import java.util.Queue;
import java.util.regex.Pattern;

/**
 * Slice from repo config.
 * @since 0.1.4
 */
public final class SliceFromConfig extends Slice.Wrap {

    /**
     * Pattern to trim path before passing it to adapters' slice.
     */
    private static final Pattern PTRN = Pattern.compile("/(?:[^/.]+)(/.*)?");

    /**
     * Ctor.
     * @param settings Artipie settings
     * @param config Repo config
     * @param standalone Standalone flag
     * @param tokens Tokens: authentication and generation
     */
    public SliceFromConfig(
        final Settings settings, final RepoConfig config,
        final boolean standalone, final Tokens tokens
    ) {
        super(
            SliceFromConfig.build(
                settings, new LoggingAuth(settings.authz().authentication()), tokens,
                settings.authz().policy(), config, standalone
            )
        );
    }

    /**
     * Find a slice implementation for config.
     *
     * @param settings Artipie settings
     * @param auth Authentication
     * @param tokens Tokens: authentication and generation
     * @param policy Security policy
     * @param cfg Repository config
     * @param standalone Standalone flag
     * @return Slice completionStage
     */
    @SuppressWarnings(
        {
            "PMD.CyclomaticComplexity", "PMD.ExcessiveMethodLength",
            "PMD.AvoidDuplicateLiterals", "PMD.NcssCount"
        }
    )
    private static Slice build(
        final Settings settings,
        final Authentication auth,
        final Tokens tokens,
        final Policy<?> policy,
        final RepoConfig cfg,
        final boolean standalone
    ) {
        final Slice slice;
        final Optional<Queue<ArtifactEvent>> events =
            settings.artifactMetadata().map(MetadataEventQueues::eventQueue);
        switch (cfg.type()) {
            case "file":
                slice = new TrimPathSlice(
                    new FilesSlice(cfg.storage(), policy, auth, cfg.name(), events),
                    SliceFromConfig.PTRN
                );
                break;
            case "file-proxy":
                slice = new TrimPathSlice(
                    new FileProxy(clientSlices(cfg, settings), cfg, events), SliceFromConfig.PTRN
                );
                break;
            case "npm":
                slice = new TrimPathSlice(
                    new NpmSlice(
                        cfg.url(), cfg.storage(), policy, tokens.auth(), cfg.name(), events
                    ),
                    SliceFromConfig.PTRN
                );
                break;
            case "gem":
                slice = new TrimPathSlice(new GemSlice(cfg.storage()), SliceFromConfig.PTRN);
                break;
            case "helm":
                slice = new TrimPathSlice(
                    new HelmSlice(
                        cfg.storage(), cfg.url().toString(), policy, auth, cfg.name(), events
                    ),
                    SliceFromConfig.PTRN
                );
                break;
            case "rpm":
                slice = new TrimPathSlice(
                    new RpmSlice(
                        cfg.storage(), policy, auth,
                        new com.artipie.rpm.RepoConfig.FromYaml(cfg.settings(), cfg.name())
                    ),
                    SliceFromConfig.PTRN
                );
                break;
            case "php":
                slice = new TrimPathSlice(
                    new PhpComposer(
                        new AstoRepository(cfg.storage(), Optional.of(cfg.url().toString())),
                        policy,
                        auth,
                        cfg.name(),
                        events
                    ),
                    SliceFromConfig.PTRN
                );
                break;
            case "php-proxy":
                slice = new TrimPathSlice(
                    new ComposerProxy(clientSlices(cfg, settings), cfg), SliceFromConfig.PTRN
                );
                break;
            case "nuget":
                slice = new TrimPathSlice(
                    new NuGet(
                        cfg.url(),
                        new com.artipie.nuget.AstoRepository(cfg.storage()),
                        policy,
                        auth,
                        cfg.name(),
                        events
                    ),
                    SliceFromConfig.PTRN
                );
                break;
            case "maven":
                slice = new TrimPathSlice(
                    new MavenSlice(cfg.storage(), policy, auth, cfg.name(), events),
                    SliceFromConfig.PTRN
                );
                break;
            case "maven-proxy":
                slice = new TrimPathSlice(
                    new MavenProxy(
                        clientSlices(cfg, settings), cfg,
                        settings.artifactMetadata().flatMap(queues -> queues.proxyEventQueues(cfg))
                    ),
                    SliceFromConfig.PTRN
                );
                break;
            case "go":
                slice = new TrimPathSlice(
                    new GoSlice(cfg.storage(), policy, auth, cfg.name()), SliceFromConfig.PTRN
                );
                break;
            case "npm-proxy":
                slice = new NpmProxySlice(
                    cfg.path(),
                    new NpmProxy(
                        URI.create(
                            cfg.settings().orElseThrow().yamlMapping("remote").string("url")
                        ),
                        cfg.storage(),
                        clientSlices(cfg, settings)
                    ),
                    settings.artifactMetadata().flatMap(queues -> queues.proxyEventQueues(cfg))
                );
                break;
            case "pypi":
                slice = new TrimPathSlice(
                    new PySlice(cfg.storage(), policy, auth, cfg.name(), events),
                    SliceFromConfig.PTRN
                );
                break;
            case "pypi-proxy":
                slice = new TrimPathSlice(
                    new PypiProxy(
                        clientSlices(cfg, settings), cfg, settings.artifactMetadata()
                            .flatMap(queues -> queues.proxyEventQueues(cfg))
                    ),
                    SliceFromConfig.PTRN
                );
                break;
            case "docker":
                final Docker docker = new AstoDocker(
                    new SubStorage(RegistryRoot.V2, cfg.storage())
                );
                if (standalone) {
                    slice = new DockerSlice(
                        docker,
                        policy,
                        new BasicAuthScheme(auth),
                        events,
                        cfg.name()
                    );
                } else {
                    slice = new DockerRoutingSlice.Reverted(
                        new DockerSlice(
                            new TrimmedDocker(docker, cfg.name()),
                            policy,
                            new BasicAuthScheme(auth),
                            events,
                            cfg.name()
                        )
                    );
                }
                break;
            case "docker-proxy":
                slice = new DockerProxy(
                    clientSlices(cfg, settings), standalone, cfg, policy, auth, events
                );
                break;
            case "deb":
                slice = new TrimPathSlice(
                    new DebianSlice(
                        cfg.storage(), policy, auth,
                        new Config.FromYaml(cfg.name(), cfg.settings(), settings.configStorage()),
                        events
                    ),
                    SliceFromConfig.PTRN
                );
                break;
            case "conda":
                slice = new CondaSlice(
                    cfg.storage(), policy, auth, tokens, cfg.url().toString(), cfg.name(), events
                );
                break;
            case "conan":
                slice = new ConanSlice(
                    cfg.storage(), policy, auth, tokens, new ItemTokenizer(Vertx.vertx()),
                    cfg.name()
                );
                break;
            case "hexpm":
                slice = new TrimPathSlice(
                    new HexSlice(cfg.storage(), policy, auth, events, cfg.name()),
                    SliceFromConfig.PTRN
                );
                break;
            default:
                throw new IllegalStateException(
                    String.format("Unsupported repository type '%s", cfg.type())
                );
        }
        return settings.caches()
            .filtersCache()
            .filters(cfg.name(), cfg.repoYaml())
            .<Slice>map(filters -> new FilterSlice(slice, filters))
            .or(() -> Optional.of(slice))
            .map(
                res ->
                    new ContinueSlice(
                        cfg.contentLengthMax()
                            .<Slice>map(limit -> new ContentLengthRestriction(res, limit))
                            .orElse(res)
                    )
            )
            .get();
    }

    private static ClientSlices clientSlices(final RepoConfig cfg, final Settings settings) {
        return new JettyClientSlices(
            cfg.httpClientSettings().orElseGet(settings::httpClientSettings)
        );
    }
}

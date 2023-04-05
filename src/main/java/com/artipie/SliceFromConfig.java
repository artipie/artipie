/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
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
import com.artipie.http.async.AsyncSlice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.auth.Tokens;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.group.GroupSlice;
import com.artipie.http.slice.TrimPathSlice;
import com.artipie.maven.http.MavenSlice;
import com.artipie.npm.http.NpmSlice;
import com.artipie.npm.proxy.NpmProxy;
import com.artipie.npm.proxy.http.NpmProxySlice;
import com.artipie.nuget.http.NuGet;
import com.artipie.pypi.http.PySlice;
import com.artipie.rpm.http.RpmSlice;
import com.artipie.security.policy.Policy;
import com.artipie.settings.Settings;
import com.artipie.settings.repo.RepoConfig;
import com.artipie.settings.repo.RepositoriesFromStorage;
import java.net.URI;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Slice from repo config.
 * @since 0.1.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNameCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 * @checkstyle CyclomaticComplexityCheck (500 lines)
 * @checkstyle ClassFanOutComplexityCheck (500 lines)
 */
public final class SliceFromConfig extends Slice.Wrap {

    /**
     * Ctor.
     * @param http HTTP client
     * @param settings Artipie settings
     * @param config Repo config
     * @param standalone Standalone flag
     * @param tokens Tokens: authentication and generation
     */
    public SliceFromConfig(
            final ClientSlices http,
            final Settings settings, final RepoConfig config,
            final boolean standalone, final Tokens tokens) {
        super(
                SliceFromConfig.build(
                        http, settings, new LoggingAuth(settings.authz().authentication()), tokens,
                        settings.authz().policy(), config, standalone
                )
        );
    }

    /**
     * Find a slice implementation for config.
     *
     * @param http HTTP client
     * @param settings Artipie settings
     * @param auth Authentication
     * @param tokens Tokens: authentication and generation
     * @param policy Security policy
     * @param cfg Repository config
     * @param standalone Standalone flag
     * @return Slice completionStage
     * @checkstyle LineLengthCheck (150 lines)
     * @checkstyle ExecutableStatementCountCheck (100 lines)
     * @checkstyle JavaNCSSCheck (500 lines)
     * @checkstyle MethodLengthCheck (500 lines)
     */
//    @SuppressWarnings(
//            {
//                    "PMD.CyclomaticComplexity", "PMD.ExcessiveMethodLength",
//                    "PMD.AvoidDuplicateLiterals", "PMD.NcssCount"
//            }
//    )
    private static Slice build(
            final ClientSlices http,
            final Settings settings,
            final Authentication auth,
            final Tokens tokens,
            final Policy<?> policy,
            final RepoConfig cfg,
            final boolean standalone
    ) {
        final Slice slice;
        switch (cfg.type()) {
            case "file":
                slice = createFileSlice(settings, policy, auth, cfg);
                break;
            case "file-proxy":
                slice = createFileProxySlice(settings, http, cfg);
                break;
            case "npm":
                slice = createNpmSlice(settings, policy, tokens, cfg);
                break;
            case "gem":
                slice = createGemSlice(settings, cfg);
                break;
            case "helm":
                slice = createHelmSlice(settings, policy, auth, cfg);
                break;
            case "rpm":
                slice = createRpmSlice(settings, policy, auth, cfg);
                break;
            case "php":
                slice = createPhpSlice(settings, cfg);
                break;
            case "php-proxy":
                slice = createPhpProxySlice(settings, http, cfg);
                break;
            case "nuget":
                slice = createNugetSlice(settings, policy, auth, cfg);
                break;
            case "maven":
                slice = createMavenSlice(settings, policy, auth, cfg);
                break;
            case "maven-proxy":
                slice = createMavenProxySlice(settings, http, cfg);
                break;
            case "maven-group":
                slice = createMavenGroupSlice( settings, http, standalone, tokens,cfg);
                break;
            case "go":
                slice = createGoSlice(settings, policy, auth, cfg);
                break;
            case "npm-proxy":
                slice = createNpmProxySlice(cfg, http);
                break;
            case "pypi":
                slice = createPypiSlice(settings, policy, auth, cfg);
                break;
            case "pypi-proxy":
                slice = createPypiProxySlice(settings, http, cfg);
                break;
            case "docker":
                slice = createDockerSlice(settings, policy, auth, cfg, standalone);
                break;
            case "docker-proxy":
                slice = createDockerProxySlice(settings, http, cfg, policy, auth, standalone);
                break;
            case "deb":
                slice = createDebSlice(settings, policy, auth, cfg);
                break;
            case "conda":
                slice = createCondaSlice(settings, policy, auth, tokens, cfg);
                break;
            case "hexpm":
                slice = createHexpmSlice(settings, policy, auth, cfg);
                break;
            default:
                throw new IllegalStateException(
                        String.format("Unsupported repository type '%s", cfg.type())
                );
        }
        return new ContinueSlice(
                cfg.contentLengthMax()
                        .<Slice>map(limit -> new ContentLengthRestriction(slice, limit))
                        .orElse(slice)
        );
    }
    private static Slice createFileSlice(Settings settings, Policy<?> policy, Authentication auth, RepoConfig cfg) {
        return new TrimPathSlice(
                new FilesSlice(cfg.storage(), policy, auth, cfg.name()), settings.layout().pattern()
        );
    }

    private static Slice createFileProxySlice(Settings settings, ClientSlices http, RepoConfig cfg) {
        return new TrimPathSlice(new FileProxy(http, cfg), settings.layout().pattern());
    }

    private static Slice createNpmSlice(Settings settings, Policy<?> policy, Tokens tokens, RepoConfig cfg) {
        return new TrimPathSlice(
                new NpmSlice(cfg.url(), cfg.storage(), policy, tokens.auth(), cfg.name()),
                settings.layout().pattern()
        );
    }

    private static Slice createGemSlice(Settings settings, RepoConfig cfg) {
        return new TrimPathSlice(new GemSlice(cfg.storage()), settings.layout().pattern());
    }

    private static Slice createHelmSlice(Settings settings, Policy<?> policy, Authentication auth, RepoConfig cfg) {
        return new TrimPathSlice(
                new HelmSlice(cfg.storage(), cfg.url().toString(), policy, auth, cfg.name()),
                settings.layout().pattern()
        );
    }

    private static Slice createRpmSlice(Settings settings, Policy<?> policy, Authentication auth, RepoConfig cfg) {
        return new TrimPathSlice(
                new RpmSlice(
                        cfg.storage(), policy, auth,
                        new com.artipie.rpm.RepoConfig.FromYaml(cfg.settings(), cfg.name())
                ),
                settings.layout().pattern()
        );
    }

    private static Slice createPhpSlice(Settings settings, RepoConfig cfg) {
        return new TrimPathSlice(
                new PhpComposer(
                        new AstoRepository(cfg.storage(), Optional.of(cfg.url().toString()))
                ),
                settings.layout().pattern()
        );
    }

    private static Slice createPhpProxySlice(Settings settings, ClientSlices http, RepoConfig cfg) {
        return new TrimPathSlice(
                new ComposerProxy(http, cfg), settings.layout().pattern()
        );
    }

    private static Slice createNugetSlice(Settings settings, Policy<?> policy, Authentication auth, RepoConfig cfg) {
        return new TrimPathSlice(
                new NuGet(
                        cfg.url(),
                        new com.artipie.nuget.AstoRepository(cfg.storage()),
                        policy,
                        auth,
                        cfg.name()
                ),
                settings.layout().pattern()
        );
    }

    private static Slice createMavenSlice(Settings settings, Policy<?> policy, Authentication auth, RepoConfig cfg) {
        return new TrimPathSlice(
                new MavenSlice(cfg.storage(), policy, auth, cfg.name()),
                settings.layout().pattern()
        );
    }

    private static Slice createMavenProxySlice(Settings settings, ClientSlices http, RepoConfig cfg) {
        return new TrimPathSlice(new MavenProxy(http, cfg), settings.layout().pattern());
    }

    private static Slice createMavenGroupSlice(Settings settings, ClientSlices http, boolean standalone, Tokens tokens, RepoConfig cfg) {
        return new TrimPathSlice(
                new GroupSlice(
                        cfg.settings().orElseThrow().yamlSequence("repositories").values()
                                .stream().map(node -> node.asScalar().value())
                                .map(
                                        name -> new AsyncSlice(
                                                new RepositoriesFromStorage(settings)
                                                        .config(name)
                                                        .thenApply(
                                                                sub -> new SliceFromConfig(
                                                                        http, settings, sub, standalone, tokens
                                                                )
                                                        )
                                        )
                                ).collect(Collectors.toList())
                ),
                settings.layout().pattern()
        );
    }

    private static Slice createGoSlice(Settings settings, Policy<?> policy, Authentication auth, RepoConfig cfg) {
        return new TrimPathSlice(
                new GoSlice(cfg.storage(), policy, auth, cfg.name()),
                settings.layout().pattern()
        );
    }

    private static Slice createNpmProxySlice(RepoConfig cfg, ClientSlices http) {
        return new NpmProxySlice(
                cfg.path(),
                new NpmProxy(
                        URI.create(
                                cfg.settings().orElseThrow().yamlMapping("remote").string("url")
                        ),
                        cfg.storage(),
                        http
                )
        );
    }

    private static Slice createPypiSlice(Settings settings, Policy<?> policy, Authentication auth, RepoConfig cfg) {
        return new TrimPathSlice(
                new PySlice(cfg.storage(), policy, auth, cfg.name()),
                settings.layout().pattern()
        );
    }

    private static Slice createPypiProxySlice(Settings settings, ClientSlices http, RepoConfig cfg) {
        return new TrimPathSlice(new PypiProxy(http, cfg), settings.layout().pattern());
    }

    private static Slice createDockerSlice(Settings settings, Policy<?> policy, Authentication auth, RepoConfig cfg, boolean standalone) {
        final Docker docker = new AstoDocker(
                new SubStorage(RegistryRoot.V2, cfg.storage())
        );
        if (standalone) {
            return new DockerSlice(
                    docker,
                    policy,
                    new BasicAuthScheme(auth),
                    cfg.name()
            );
        } else {
            return new DockerRoutingSlice.Reverted(
                    new DockerSlice(
                            new TrimmedDocker(docker, cfg.name()),
                            policy,
                            new BasicAuthScheme(auth),
                            cfg.name()
                    )
            );
        }
    }

    private static Slice createDockerProxySlice(Settings settings, ClientSlices http, RepoConfig cfg, Policy<?> policy, Authentication auth, boolean standalone) {
        return new DockerProxy(http, standalone, cfg, policy, auth);
    }

    private static Slice createDebSlice(Settings settings, Policy<?> policy, Authentication auth, RepoConfig cfg) {
        return new TrimPathSlice(
                new DebianSlice(
                        cfg.storage(), policy, auth,
                        new Config.FromYaml(cfg.name(), cfg.settings(), settings.configStorage())
                ),
                settings.layout().pattern()
        );
    }

    private static Slice createCondaSlice(Settings settings, Policy<?> policy, Authentication auth, Tokens tokens, RepoConfig cfg) {
        return new CondaSlice(
                cfg.storage(), policy, auth, tokens, cfg.url().toString(), cfg.name()
        );
    }

    private static Slice createHexpmSlice(Settings settings, Policy<?> policy, Authentication auth, RepoConfig cfg) {
        return new TrimPathSlice(
                new HexSlice(cfg.storage(), policy, auth, cfg.name()),
                settings.layout().pattern()
        );
    }

}



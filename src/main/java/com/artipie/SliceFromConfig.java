/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie;

import com.artipie.adapters.docker.DockerPermissions;
import com.artipie.adapters.docker.DockerProxy;
import com.artipie.adapters.file.FileProxy;
import com.artipie.adapters.maven.MavenProxy;
import com.artipie.adapters.php.ComposerProxy;
import com.artipie.adapters.pypi.PypiProxy;
import com.artipie.asto.SubStorage;
import com.artipie.auth.LoggingAuth;
import com.artipie.auth.LoggingPermissions;
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
import com.artipie.http.auth.Permissions;
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
                http, settings, new LoggingAuth(settings.auth()),
                tokens, config, standalone
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
     * @param cfg Repository config
     * @param standalone Standalone flag
     * @return Slice completionStage
     * @checkstyle LineLengthCheck (150 lines)
     * @checkstyle ExecutableStatementCountCheck (100 lines)
     * @checkstyle JavaNCSSCheck (500 lines)
     * @checkstyle MethodLengthCheck (500 lines)
     */
    @SuppressWarnings(
        {
            "PMD.CyclomaticComplexity", "PMD.ExcessiveMethodLength",
            "PMD.AvoidDuplicateLiterals", "PMD.NcssCount"
        }
    )
    private static Slice build(
        final ClientSlices http,
        final Settings settings,
        final Authentication auth,
        final Tokens tokens,
        final RepoConfig cfg,
        final boolean standalone
    ) {
        final Slice slice;
        final Permissions permissions = new LoggingPermissions(
            cfg.permissions().orElse(Permissions.FREE)
        );
        switch (cfg.type()) {
            case "file":
                slice = new TrimPathSlice(
                    new FilesSlice(cfg.storage(), permissions, auth), settings.layout().pattern()
                );
                break;
            case "file-proxy":
                slice = new TrimPathSlice(new FileProxy(http, cfg), settings.layout().pattern());
                break;
            case "npm":
                slice = new TrimPathSlice(
                    new NpmSlice(cfg.url(), cfg.storage(), permissions, tokens.auth()),
                    settings.layout().pattern()
                );
                break;
            case "gem":
                slice = new TrimPathSlice(new GemSlice(cfg.storage()), settings.layout().pattern());
                break;
            case "helm":
                slice = new TrimPathSlice(
                    new HelmSlice(cfg.storage(), cfg.url().toString(), permissions, auth),
                    settings.layout().pattern()
                );
                break;
            case "rpm":
                slice = new TrimPathSlice(
                    new RpmSlice(
                        cfg.storage(), permissions, auth,
                        new com.artipie.rpm.RepoConfig.FromYaml(cfg.settings())
                    ),
                    settings.layout().pattern()
                );
                break;
            case "php":
                slice = new TrimPathSlice(
                    new PhpComposer(
                        new AstoRepository(cfg.storage(), Optional.of(cfg.url().toString()))
                    ),
                    settings.layout().pattern()
                );
                break;
            case "php-proxy":
                slice = new TrimPathSlice(
                    new ComposerProxy(http, cfg), settings.layout().pattern()
                );
                break;
            case "nuget":
                slice = new TrimPathSlice(
                    new NuGet(
                        cfg.url(),
                        new com.artipie.nuget.AstoRepository(cfg.storage()),
                        permissions,
                        auth
                    ),
                    settings.layout().pattern()
                );
                break;
            case "maven":
                slice = new TrimPathSlice(
                    new MavenSlice(cfg.storage(), permissions, auth), settings.layout().pattern()
                );
                break;
            case "maven-proxy":
                slice = new TrimPathSlice(new MavenProxy(http, cfg), settings.layout().pattern());
                break;
            case "maven-group":
                slice = new TrimPathSlice(
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
                break;
            case "go":
                slice = new TrimPathSlice(
                    new GoSlice(cfg.storage(), permissions, auth), settings.layout().pattern()
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
                        http
                    )
                );
                break;
            case "pypi":
                slice = new TrimPathSlice(
                    new PySlice(cfg.storage(), permissions, auth), settings.layout().pattern()
                );
                break;
            case "pypi-proxy":
                slice = new TrimPathSlice(new PypiProxy(http, cfg), settings.layout().pattern());
                break;
            case "docker":
                final Docker docker = new AstoDocker(
                    new SubStorage(RegistryRoot.V2, cfg.storage())
                );
                if (standalone) {
                    slice = new DockerSlice(
                        docker,
                        new DockerPermissions(permissions),
                        new BasicAuthScheme(auth)
                    );
                } else {
                    slice = new DockerRoutingSlice.Reverted(
                        new DockerSlice(
                            new TrimmedDocker(docker, cfg.name()),
                            new DockerPermissions(permissions),
                            new BasicAuthScheme(auth)
                        )
                    );
                }
                break;
            case "docker-proxy":
                slice = new DockerProxy(http, standalone, cfg, permissions, auth);
                break;
            case "deb":
                slice = new TrimPathSlice(
                    new DebianSlice(
                        cfg.storage(), permissions, auth,
                        new Config.FromYaml(cfg.name(), cfg.settings(), settings.configStorage())
                    ),
                    settings.layout().pattern()
                );
                break;
            case "conda":
                slice = new CondaSlice(
                    cfg.storage(), permissions, auth, tokens, cfg.url().toString()
                );
                break;
            case "hexpm":
                slice = new TrimPathSlice(
                    new HexSlice(cfg.storage(), permissions, auth),
                    settings.layout().pattern()
                );
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
}

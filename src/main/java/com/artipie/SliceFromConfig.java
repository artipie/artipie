/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie;

import com.artipie.asto.SubStorage;
import com.artipie.auth.LoggingAuth;
import com.artipie.composer.AstoRepository;
import com.artipie.composer.http.PhpComposer;
import com.artipie.debian.Config;
import com.artipie.debian.http.DebianSlice;
import com.artipie.docker.Docker;
import com.artipie.docker.DockerPermissions;
import com.artipie.docker.DockerProxy;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.asto.RegistryRoot;
import com.artipie.docker.http.DockerSlice;
import com.artipie.docker.http.TrimmedDocker;
import com.artipie.file.FileProxy;
import com.artipie.files.FilesSlice;
import com.artipie.gem.GemSlice;
import com.artipie.helm.HelmSlice;
import com.artipie.http.DockerRoutingSlice;
import com.artipie.http.GoSlice;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncSlice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.auth.Permissions;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.group.GroupSlice;
import com.artipie.http.slice.TrimPathSlice;
import com.artipie.maven.MavenProxy;
import com.artipie.maven.http.MavenSlice;
import com.artipie.npm.http.NpmSlice;
import com.artipie.npm.proxy.NpmProxy;
import com.artipie.npm.proxy.NpmProxyConfig;
import com.artipie.npm.proxy.http.NpmProxySlice;
import com.artipie.nuget.http.NuGet;
import com.artipie.php.ComposerProxy;
import com.artipie.pypi.PypiProxy;
import com.artipie.pypi.http.PySlice;
import com.artipie.rpm.http.RpmSlice;
import io.vertx.reactivex.core.Vertx;
import java.util.ArrayList;
import java.util.stream.Collectors;
import org.jruby.javasupport.JavaEmbedUtils;

/**
 * Slice from repo config.
 * @since 0.1.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNameCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 * @checkstyle CyclomaticComplexityCheck (500 lines)
 * @checkstyle ClassFanOutComplexityCheck (500 lines)
 */
@SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.StaticAccessToStaticFields"})
public final class SliceFromConfig extends Slice.Wrap {

    /**
     * Http client.
     * @todo #213:30min HTTP client should not be a singleton.
     *  HTTP client now is a singleton within application.
     *  It's instance is stored in static variable.
     *  It should be refactored so it's only instance is built on start of app
     *  and passed from top level to this class and other usages.
     */
    public static final JettyClientSlices HTTP;

    static {
        HTTP = new JettyClientSlices(new HttpClientSettings());
        try {
            SliceFromConfig.HTTP.start();
            // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Exception err) {
            throw new IllegalStateException(err);
        }
    }

    /**
     * Ctor.
     * @param settings Artipie settings
     * @param config Repo config
     * @param aliases Storage aliases
     * @param standalone Standalone flag
     */
    public SliceFromConfig(
        final Settings settings, final RepoConfig config,
        final StorageAliases aliases,
        final boolean standalone) {
        super(
            new AsyncSlice(
                settings.auth().thenApply(
                    auth -> SliceFromConfig.build(
                        settings, new LoggingAuth(auth),
                        config, aliases, standalone
                    )
                )
            )
        );
    }

    /**
     * Find a slice implementation for config.
     *
     * @param settings Artipie settings
     * @param auth Authentication
     * @param cfg Repository config
     * @param aliases Storage aliases
     * @param standalone Standalone flag
     * @return Slice completionStage
     * @todo #90:30min This method still needs more refactoring.
     *  We should test if the type exist in the constructed map. If the type does not exist,
     *  we should throw an IllegalStateException with the message "Unsupported repository type '%s'"
     * @todo #738:30min Remove creating a Vert.x instance in npm-proxy case.
     *  Vertx.vertx() call creates a Vert.x instance, a very heavy object. It should be
     *  created only once for whole application on start and reused everywhere.
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
    static Slice build(
        final Settings settings, final Authentication auth,
        final RepoConfig cfg, final StorageAliases aliases, final boolean standalone) {
        final Slice slice;
        final Permissions permissions = new LoggingPermissions(
            cfg.permissions().orElse(Permissions.FREE)
        );
        switch (cfg.type()) {
            case "file":
                slice = trimIfNotStandalone(
                    settings, standalone,
                    new FilesSlice(cfg.storage(), permissions, auth)
                );
                break;
            case "file-proxy":
                slice = trimIfNotStandalone(
                    settings, standalone,
                    new FileProxy(SliceFromConfig.HTTP, cfg)
                );
                break;
            case "npm":
                slice = trimIfNotStandalone(
                    settings, standalone,
                    new NpmSlice(
                        cfg.url(),
                        cfg.storage(),
                        permissions,
                        auth
                    )
                );
                break;
            case "gem":
                slice = trimIfNotStandalone(
                    settings, standalone,
                    new GemSlice(
                        cfg.storage(),
                        JavaEmbedUtils.initialize(new ArrayList<>(0)),
                        permissions,
                        auth
                    )
                );
                break;
            case "helm":
                slice = trimIfNotStandalone(
                    settings, standalone,
                    new HelmSlice(
                        cfg.storage(),
                        cfg.path(),
                        permissions,
                        auth
                    )
                );
                break;
            case "rpm":
                slice = trimIfNotStandalone(
                    settings, standalone,
                    new RpmSlice(
                        cfg.storage(), permissions, auth,
                        new com.artipie.rpm.RepoConfig.FromYaml(cfg.settings())
                    )
                );
                break;
            case "php":
                slice = trimIfNotStandalone(
                    settings, standalone, new PhpComposer(new AstoRepository(cfg.storage()))
                );
                break;
            case "php-proxy":
                slice = trimIfNotStandalone(
                    settings,
                    standalone,
                    new ComposerProxy(SliceFromConfig.HTTP, cfg)
                );
                break;
            case "nuget":
                slice = trimIfNotStandalone(
                    settings,
                    standalone,
                    new NuGet(
                        cfg.url(),
                        new com.artipie.nuget.AstoRepository(cfg.storage()),
                        permissions,
                        auth
                    )
                );
                break;
            case "maven":
                slice = trimIfNotStandalone(
                    settings, standalone,
                    new MavenSlice(cfg.storage(), permissions, auth)
                );
                break;
            case "maven-proxy":
                slice = trimIfNotStandalone(
                    settings, standalone,
                    new MavenProxy(SliceFromConfig.HTTP, cfg)
                );
                break;
            case "maven-group":
                slice = trimIfNotStandalone(
                    settings, standalone,
                    new GroupSlice(
                        cfg.settings().orElseThrow().yamlSequence("repositories").values()
                            .stream().map(node -> node.asScalar().value())
                            .map(
                                name -> new AsyncSlice(
                                    new RepositoriesFromStorage(settings.storage()).config(name)
                                        .thenApply(
                                            sub -> new SliceFromConfig(
                                                settings, sub, aliases, standalone
                                            )
                                        )
                                )
                            ).collect(Collectors.toList())
                    )
                );
                break;
            case "go":
                slice = trimIfNotStandalone(
                    settings, standalone,
                    new GoSlice(cfg.storage(), permissions, auth)
                );
                break;
            case "npm-proxy":
                slice = new NpmProxySlice(
                    cfg.path(),
                    new NpmProxy(
                        new NpmProxyConfig(cfg.settings().orElseThrow()),
                        Vertx.vertx(),
                        cfg.storage()
                    )
                );
                break;
            case "pypi":
                slice = trimIfNotStandalone(
                    settings, standalone,
                    new PySlice(cfg.storage(), permissions, auth)
                );
                break;
            case "pypi-proxy":
                slice = trimIfNotStandalone(
                    settings, standalone,
                    new PypiProxy(SliceFromConfig.HTTP, cfg)
                );
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
                slice = new DockerProxy(SliceFromConfig.HTTP, standalone, cfg, permissions, auth);
                break;
            case "deb":
                slice = trimIfNotStandalone(
                    settings, standalone,
                    new DebianSlice(
                        cfg.storage(), permissions, auth,
                        new Config.FromYaml(cfg.name(), cfg.settings(), settings.storage())
                    )
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

    /**
     * Wraps origin slice if into TrimPathSlice if it is not standalone.
     *
     * @param settings Artipie settings
     * @param standalone Standalone flag
     * @param origin Origin slice
     * @return Origin slice wrapped if needed
     */
    private static Slice trimIfNotStandalone(
        final Settings settings, final boolean standalone, final Slice origin
    ) {
        final Slice result;
        if (standalone) {
            result = origin;
        } else {
            result = new TrimPathSlice(origin, settings.layout().pattern());
        }
        return result;
    }
}

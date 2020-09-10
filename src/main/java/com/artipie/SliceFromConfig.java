/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.artipie;

import com.artipie.asto.Key;
import com.artipie.auth.JoinedPermissions;
import com.artipie.auth.LoggingAuth;
import com.artipie.composer.http.PhpComposer;
import com.artipie.docker.DockerProxy;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.http.DockerSlice;
import com.artipie.docker.http.TrimmedDocker;
import com.artipie.files.FileProxySlice;
import com.artipie.files.FilesSlice;
import com.artipie.gem.GemSlice;
import com.artipie.helm.HelmSlice;
import com.artipie.http.DockerRoutingSlice;
import com.artipie.http.GoSlice;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncSlice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicIdentities;
import com.artipie.http.auth.Permissions;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.group.GroupSlice;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.http.slice.TrimPathSlice;
import com.artipie.maven.http.MavenProxySlice;
import com.artipie.maven.http.MavenSlice;
import com.artipie.maven.repository.StorageCache;
import com.artipie.npm.Npm;
import com.artipie.npm.http.NpmSlice;
import com.artipie.npm.proxy.NpmProxy;
import com.artipie.npm.proxy.NpmProxyConfig;
import com.artipie.npm.proxy.http.NpmProxySlice;
import com.artipie.nuget.http.NuGet;
import com.artipie.pypi.http.PySlice;
import com.artipie.repo.PathPattern;
import com.artipie.rpm.http.RpmSlice;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     */
    private static final JettyClientSlices HTTP;

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
     */
    public SliceFromConfig(final Settings settings, final RepoConfig config,
        final StorageAliases aliases) {
        super(
            new AsyncSlice(
                settings.permissions().thenCombine(
                    settings.auth(),
                    (gpermissions, auth) -> SliceFromConfig.build(
                        settings,
                        new LoggingPermissions(
                            new JoinedPermissions(
                                Stream.concat(
                                    gpermissions.stream().map(gp -> gp.permissions(config.name())),
                                    config.permissions().stream()
                                ).collect(Collectors.toList())
                            )
                        ),
                        new LoggingAuth(auth),
                        config,
                        aliases
                    )
                )
            )
        );
    }

    /**
     * Find a slice implementation for config.
     *
     * @param settings Artipie settings
     * @param permissions Permissions
     * @param auth Authentication
     * @param cfg Repository config
     * @param aliases Storage aliases
     * @return Slice completionStage
     * @todo #90:30min This method still needs more refactoring.
     *  We should test if the type exist in the constructed map. If the type does not exist,
     *  we should throw an IllegalStateException with the message "Unsupported repository type '%s'"
     * @checkstyle LineLengthCheck (150 lines)
     * @checkstyle ExecutableStatementCountCheck (100 lines)
     * @checkstyle JavaNCSSCheck (500 lines)
     */
    @SuppressWarnings(
        {
            "PMD.CyclomaticComplexity", "PMD.ExcessiveMethodLength",
            "PMD.AvoidDuplicateLiterals", "PMD.NcssCount"
        }
    )
    static Slice build(
        final Settings settings,
        final Permissions permissions,
        final Authentication auth,
        final RepoConfig cfg,
        final StorageAliases aliases) {
        final Slice slice;
        final Pattern prefix = new PathPattern(settings).pattern();
        switch (cfg.type()) {
            case "file":
                slice = new TrimPathSlice(new FilesSlice(cfg.storage(), permissions, auth), prefix);
                break;
            case "file-proxy":
                slice = new TrimPathSlice(
                    new FileProxySlice(
                        SliceFromConfig.HTTP,
                        URI.create(
                            cfg.settings()
                                .orElseThrow(
                                    () -> new IllegalStateException("Repo settings missed")
                                ).string("remote_uri")
                        )
                    ), prefix
                );
                break;
            case "npm":
                slice = new NpmSlice(
                    cfg.path(),
                    new Npm(cfg.storage()),
                    cfg.storage(),
                    permissions,
                    new BasicIdentities(auth)
                );
                break;
            case "gem":
                slice = new TrimPathSlice(new GemSlice(cfg.storage()), prefix);
                break;
            case "helm":
                slice = new TrimPathSlice(
                    new HelmSlice(
                        cfg.storage(),
                        cfg.path(),
                        permissions,
                        new BasicIdentities(auth)
                    ),
                    prefix
                );
                break;
            case "rpm":
                slice = new TrimPathSlice(
                    new RpmSlice(
                        cfg.storage(), permissions, new BasicIdentities(auth),
                        new com.artipie.rpm.RepoConfig.FromYaml(cfg.settings())
                    ), prefix
                );
                break;
            case "php":
                slice = new PhpComposer(cfg.path(), cfg.storage());
                break;
            case "nuget":
                slice = new TrimPathSlice(
                    new NuGet(cfg.url(), cfg.storage(), permissions, auth),
                    prefix
                );
                break;
            case "maven":
                slice = new TrimPathSlice(new MavenSlice(cfg.storage(), permissions, auth), prefix);
                break;
            case "maven-proxy":
                slice = new TrimPathSlice(
                    new MavenProxySlice(
                        SliceFromConfig.HTTP.client(),
                        URI.create(
                            cfg.settings()
                                .orElseThrow(() -> new IllegalStateException("Repo settings missed"))
                                .string("remote_uri")
                        ),
                        new StorageCache(cfg.storage())
                    ),
                    prefix
                );
                break;
            case "maven-group":
                slice = new TrimPathSlice(
                    new GroupSlice(
                        cfg.settings().orElseThrow().yamlSequence("repositories").values()
                            .stream().map(node -> node.asScalar().value())
                            .map(
                                name -> {
                                    try {
                                        return new AsyncSlice(
                                            settings.storage().value(new Key.From(String.format("%s.yaml", name)))
                                                .thenCompose(data -> RepoConfig.fromPublisher(aliases, new KeyFromPath(name), data))
                                                .thenApply(sub -> new SliceFromConfig(settings, sub, aliases))
                                        );
                                    } catch (final IOException err) {
                                        throw new UncheckedIOException(err);
                                    }
                                }
                            ).collect(Collectors.toList())
                    ),
                    prefix
                );
                break;
            case "go":
                slice = new TrimPathSlice(new GoSlice(cfg.storage()), prefix);
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
                slice = new TrimPathSlice(new PySlice(cfg.storage(), permissions, auth), prefix);
                break;
            case "docker":
                slice = new DockerRoutingSlice.Reverted(
                    new DockerSlice(
                        new TrimmedDocker(new AstoDocker(cfg.storage()), cfg.name()),
                        permissions,
                        auth
                    )
                );
                break;
            case "docker-proxy":
                slice = new DockerProxy(SliceFromConfig.HTTP, cfg, permissions, auth);
                break;
            default:
                throw new IllegalStateException(
                    String.format("Unsupported repository type '%s", cfg.type())
                );
        }
        return cfg.contentLengthMax()
            .<Slice>map(limit -> new ContentLengthRestriction(slice, limit))
            .orElse(slice);
    }
}

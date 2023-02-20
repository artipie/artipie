/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie;

import com.artipie.api.RestApi;
import com.artipie.asto.Key;
import com.artipie.auth.JwtTokens;
import com.artipie.http.ArtipieRepositories;
import com.artipie.http.BaseSlice;
import com.artipie.http.MainSlice;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.misc.ArtipieProperties;
import com.artipie.settings.ConfigFile;
import com.artipie.settings.MetricsContext;
import com.artipie.settings.Settings;
import com.artipie.settings.SettingsFromPath;
import com.artipie.settings.repo.RepoConfig;
import com.artipie.settings.repo.RepositoriesFromStorage;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Vertx server entry point.
 * @since 1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ExecutableStatementCountCheck (500 lines)
 * @checkstyle ClassFanOutComplexityCheck (500 lines)
 */
@SuppressWarnings("PMD.PrematureDeclaration")
public final class VertxMain {

    /**
     * Default port to start Artipie Rest API service.
     */
    private static final String DEF_API_PORT = "8086";

    /**
     * HTTP client.
     */
    private final ClientSlices http;

    /**
     * Config file path.
     */
    private final Path config;

    /**
     * Server port.
     */
    private final int port;

    /**
     * Servers.
     */
    private final List<VertxSliceServer> servers;

    /**
     * Ctor.
     *
     * @param http HTTP client
     * @param config Config file path.
     * @param port HTTP port
     */
    public VertxMain(final ClientSlices http, final Path config, final int port) {
        this.http = http;
        this.config = config;
        this.port = port;
        this.servers = new ArrayList<>(0);
    }

    /**
     * Starts the server.
     *
     * @param apiport Port to run Rest API service on
     * @return Port the servers listening on.
     * @throws IOException In case of error reading settings.
     */
    public int start(final int apiport) throws IOException {
        final Settings settings = new SettingsFromPath(this.config).find();
        final Vertx vertx = VertxMain.vertx(settings.metrics());
        final JWTAuth jwt = JWTAuth.create(
            vertx.getDelegate(), new JWTAuthOptions().addPubSecKey(
                new PubSecKeyOptions().setAlgorithm("HS256").setBuffer("some secret")
            )
        );
        final int main = this.listenOn(
            new MainSlice(this.http, settings, new JwtTokens(jwt)),
            this.port,
            vertx,
            settings.metrics()
        );
        Logger.info(VertxMain.class, "Artipie was started on port %d", main);
        this.startRepos(vertx, settings, this.port, jwt);
        vertx.deployVerticle(new RestApi(settings, apiport, jwt));
        return main;
    }

    /**
     * Stops server releasing all resources.
     */
    public void stop() {
        for (final VertxSliceServer server : this.servers) {
            server.stop();
        }
    }

    /**
     * Entry point.
     * @param args CLI args
     * @throws Exception If fails
     */
    public static void main(final String... args) throws Exception {
        final Path config;
        final int port;
        final int defp = 80;
        final Options options = new Options();
        final String popt = "p";
        final String fopt = "f";
        final String apiport = "ap";
        options.addOption(popt, "port", true, "The port to start Artipie on");
        options.addOption(fopt, "config-file", true, "The path to Artipie configuration file");
        options.addOption(apiport, "api-port", true, "The port to start Artipie Rest API on");
        final CommandLineParser parser = new DefaultParser();
        final CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption(popt)) {
            port = Integer.parseInt(cmd.getOptionValue(popt));
        } else {
            Logger.info(VertxMain.class, "Using default port: %d", defp);
            port = defp;
        }
        if (cmd.hasOption(fopt)) {
            config = Path.of(cmd.getOptionValue(fopt));
        } else {
            throw new IllegalStateException("Storage is not configured");
        }
        Logger.info(
            VertxMain.class,
            "Used version of Artipie: %s",
            new ArtipieProperties().version()
        );
        final JettyClientSlices http = new JettyClientSlices(new HttpClientSettings());
        http.start();
        new VertxMain(http, config, port)
            .start(Integer.parseInt(cmd.getOptionValue(apiport, VertxMain.DEF_API_PORT)));
    }

    /**
     * Start repository servers.
     *
     * @param vertx Vertx instance
     * @param settings Settings.
     * @param mport Artipie service main port
     * @param jwt Jwt authentication
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    private void startRepos(
        final Vertx vertx,
        final Settings settings,
        final int mport,
        final JWTAuth jwt
    ) {
        final Collection<RepoConfig> configs = settings.repoConfigsStorage().list(Key.ROOT)
            .thenApply(
                keys -> keys.stream().map(ConfigFile::new)
                    .filter(Predicate.not(ConfigFile::isSystem).and(ConfigFile::isYamlOrYml))
                    .map(ConfigFile::name)
                    .map(name -> new RepositoriesFromStorage(settings).config(name))
                    .map(stage -> stage.toCompletableFuture().join())
                    .collect(Collectors.toList())
            ).toCompletableFuture().join();
        for (final RepoConfig repo : configs) {
            try {
                repo.port().ifPresentOrElse(
                    prt -> {
                        final String name = new ConfigFile(repo.name()).name();
                        this.listenOn(
                            new ArtipieRepositories(
                                this.http, settings, new JwtTokens(jwt)
                            ).slice(new Key.From(name), prt),
                            prt, vertx, settings.metrics()
                        );
                        VertxMain.logRepo(prt, name);
                    },
                    () -> VertxMain.logRepo(mport, repo.name())
                );
            } catch (final IllegalStateException err) {
                Logger.error(this, "Invalid repo config file %s: %[exception]s", repo.name(), err);
            }
        }
    }

    /**
     * Starts HTTP server listening on specified port.
     *
     * @param slice Slice.
     * @param sport Slice server port.
     * @param vertx Vertx instance
     * @param mctx Metrics context
     * @return Port server started to listen on.
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    private int listenOn(
        final Slice slice, final int sport, final Vertx vertx, final MetricsContext mctx
    ) {
        final VertxSliceServer server = new VertxSliceServer(
            vertx, new BaseSlice(mctx, slice), sport
        );
        this.servers.add(server);
        return server.start();
    }

    /**
     * Log repository on start.
     * @param mport Repository port
     * @param name Repository name
     */
    private static void logRepo(final int mport, final String name) {
        Logger.info(
            VertxMain.class, "Artipie repo '%s' was started on port %d", name, mport
        );
    }

    /**
     * Obtain and configure Vert.x instance. If vertx metrics are configured,
     * this method enables Micrometer metrics options with Prometheus. Check
     * <a href="https://vertx.io/docs/3.9.13/vertx-micrometer-metrics/java/#_prometheus">docs</a>.
     * @param mctx Metrics context
     * @return Vert.x instance
     */
    private static Vertx vertx(final MetricsContext mctx) {
        final Vertx res;
        final Optional<Pair<String, Integer>> endpoint = mctx.endpointAndPort();
        if (endpoint.isPresent()) {
            res = Vertx.vertx(
                new VertxOptions().setMetricsOptions(
                    new MicrometerMetricsOptions()
                        .setPrometheusOptions(
                            new VertxPrometheusOptions().setEnabled(true)
                                .setStartEmbeddedServer(true)
                                .setEmbeddedServerOptions(
                                    new HttpServerOptions().setPort(endpoint.get().getValue())
                                ).setEmbeddedServerEndpoint(endpoint.get().getKey())
                        ).setEnabled(true)
                )
            );
            if (mctx.jvm()) {
                final MeterRegistry registry = BackendRegistries.getDefaultNow();
                new ClassLoaderMetrics().bindTo(registry);
                new JvmMemoryMetrics().bindTo(registry);
                new JvmGcMetrics().bindTo(registry);
                new ProcessorMetrics().bindTo(registry);
                new JvmThreadMetrics().bindTo(registry);
            }
            Logger.info(
                VertxMain.class,
                String.format(
                    "Monitoring is enabled, prometheus metrics are available on localhost:%d%s",
                    endpoint.get().getValue(), endpoint.get().getKey()
                )
            );
        } else {
            res = Vertx.vertx();
        }
        return res;
    }
}

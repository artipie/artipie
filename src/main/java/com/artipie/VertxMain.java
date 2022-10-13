/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie;

import com.artipie.api.RestApi;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.ArtipieRepositories;
import com.artipie.http.BaseSlice;
import com.artipie.http.MainSlice;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.metrics.Metrics;
import com.artipie.metrics.MetricsFromConfig;
import com.artipie.metrics.nop.NopMetrics;
import com.artipie.misc.ArtipieProperties;
import com.artipie.settings.ConfigFile;
import com.artipie.settings.Settings;
import com.artipie.settings.SettingsFromPath;
import com.artipie.settings.cache.SettingsCaches;
import com.artipie.settings.repo.RepoConfig;
import com.artipie.settings.repo.RepositoriesFromStorage;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
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
        final SettingsCaches caches = new SettingsCaches.All();
        final Settings settings = new SettingsFromPath(this.config).find(this.port, caches);
        final Optional<MetricsFromConfig> msettings = metricsSettings(settings);
        final Metrics metrics = msettings.map(MetricsFromConfig::metrics)
            .orElse(NopMetrics.INSTANCE);
        final Vertx vertx = VertxMain.vertx(msettings);
        final int main = this.listenOn(
            new MainSlice(this.http, settings, metrics), metrics, this.port, vertx
        );
        Logger.info(VertxMain.class, "Artipie was started on port %d", main);
        this.startRepos(vertx, settings, metrics, this.port);
        settings.auth().thenAccept(
            auth -> vertx.deployVerticle(
                new RestApi(
                    caches, settings.repoConfigsStorage(), settings.layout().toString(),
                    apiport, settings.credentialsKey(), auth, settings.keyStore()
                )
            )
        );
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
     * @param vertx Vertx instance
     * @param settings Settings.
     * @param metrics Metrics.
     * @param mport Artipie service main port
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    private void startRepos(
        final Vertx vertx, final Settings settings, final Metrics metrics, final int mport
    ) {
        final Storage storage = settings.repoConfigsStorage();
        final Collection<RepoConfig> configs = storage.list(Key.ROOT).thenApply(
            keys -> keys.stream().map(ConfigFile::new)
                .filter(Predicate.not(ConfigFile::isSystem).and(ConfigFile::isYamlOrYml))
                .map(ConfigFile::name)
                .map(name -> new RepositoriesFromStorage(storage).config(name))
                .map(stage -> stage.toCompletableFuture().join())
                .collect(Collectors.toList())
        ).toCompletableFuture().join();
        for (final RepoConfig repo : configs) {
            try {
                repo.port().ifPresentOrElse(
                    prt -> {
                        final String name = new ConfigFile(repo.name()).name();
                        this.listenOn(
                            new ArtipieRepositories(this.http, settings).slice(
                                new Key.From(name), prt
                            ), metrics, prt, vertx
                        );
                        VertxMain.logRepo(prt, name);
                    },
                    () -> VertxMain.logRepo(mport, repo.name())
                );
            } catch (final IllegalStateException err) {
                Logger.error(this, "Invalid repo config file %s: %[exception]s", repo.name(), err);
            }
        }
        new QuartzScheduler(configs).start();
    }

    /**
     * Starts HTTP server listening on specified port.
     *
     * @param slice Slice.
     * @param metrics Metrics.
     * @param sport Slice server port.
     * @param vertx Vertx instance
     * @return Port server started to listen on.
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    private int listenOn(
        final Slice slice, final Metrics metrics, final int sport, final Vertx vertx
    ) {
        final VertxSliceServer server = new VertxSliceServer(
            vertx, new BaseSlice(metrics, slice), sport
        );
        this.servers.add(server);
        return server.start();
    }

    /**
     * Creates and initialize metrics from settings.
     *
     * @param settings Settings.
     * @return Metrics.
     */
    private static Optional<MetricsFromConfig> metricsSettings(final Settings settings) {
        return Optional.ofNullable(settings.meta())
            .flatMap(meta -> Optional.ofNullable(meta.yamlSequence("metrics")))
            .map(MetricsFromConfig::new);
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
     * @param metrics Metrics if configured
     * @return Vert.x instance
     */
    private static Vertx vertx(final Optional<MetricsFromConfig> metrics) {
        return metrics.flatMap(
            sntgs -> sntgs.vertxMetricsConf().map(
                pair -> Vertx.vertx(
                    new VertxOptions().setMetricsOptions(
                        new MicrometerMetricsOptions()
                            .setPrometheusOptions(
                                new VertxPrometheusOptions().setEnabled(true)
                                    .setStartEmbeddedServer(true)
                                    .setEmbeddedServerOptions(
                                        new HttpServerOptions().setPort(pair.getValue())
                                    ).setEmbeddedServerEndpoint(pair.getKey())
                            ).setEnabled(true)
                    )
                )
            )
        ).orElse(Vertx.vertx());
    }
}

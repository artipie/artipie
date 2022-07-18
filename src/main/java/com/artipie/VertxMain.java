/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie;

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
import com.artipie.repo.ConfigFile;
import com.artipie.repo.RepoConfig;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
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
 */
@SuppressWarnings("PMD.PrematureDeclaration")
public final class VertxMain {

    /**
     * HTTP client.
     */
    private final ClientSlices http;

    /**
     * The Vert.x instance.
     */
    private final Vertx vertx;

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
     * @param vertx The Vert.x instance.
     * @param port HTTP port
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    public VertxMain(
        final ClientSlices http, final Path config,
        final Vertx vertx, final int port
    ) {
        this.http = http;
        this.config = config;
        this.vertx = vertx;
        this.port = port;
        this.servers = new ArrayList<>(0);
    }

    /**
     * Starts the server.
     *
     * @return Port the servers listening on.
     * @throws IOException In case of error reading settings.
     */
    public int start() throws IOException {
        final Settings settings = new SettingsFromPath(this.config).find(this.port);
        final Metrics metrics = metrics(settings);
        final int main = this.listenOn(
            new MainSlice(this.http, settings, metrics),
            metrics, this.port
        );
        Logger.info(VertxMain.class, "Artipie was started on port %d", main);
        this.startRepos(settings, metrics);
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
        final Vertx vertx = Vertx.vertx();
        final Path config;
        final int port;
        final int defp = 80;
        final Options options = new Options();
        final String popt = "p";
        final String fopt = "f";
        options.addOption(popt, "port", true, "The port to start artipie on");
        options.addOption(fopt, "config-file", true, "The path to artipie configuration file");
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
        new VertxMain(http, config, vertx, port).start();
    }

    /**
     * Start repository servers.
     *
     * @param settings Settings.
     * @param metrics Metrics.
     */
    private void startRepos(final Settings settings, final Metrics metrics) {
        final Storage storage = settings.repoConfigsStorage();
        final Collection<RepoConfig> configs = storage.list(Key.ROOT).thenApply(
            keys -> keys.stream().map(key -> new ConfigFile(key))
                .filter(Predicate.not(ConfigFile::isSystem).and(ConfigFile::isYamlOrYml))
                .map(ConfigFile::name)
                .map(name -> new RepositoriesFromStorage(this.http, storage).config(name))
                .map(stage -> stage.toCompletableFuture().join())
                .collect(Collectors.toList())
        ).toCompletableFuture().join();
        for (final RepoConfig repo : configs) {
            try {
                repo.port().ifPresent(
                    prt -> {
                        final String name = new ConfigFile(repo.name()).name();
                        this.listenOn(
                            new ArtipieRepositories(this.http, settings).slice(
                                new Key.From(name), prt
                            ), metrics, prt
                        );
                        Logger.info(
                            VertxMain.class, "Artipie repo '%s' was started on port %d", name, prt
                        );
                    }
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
     * @param sport Server port.
     * @return Port server started to listen on.
     */
    private int listenOn(final Slice slice, final Metrics metrics, final int sport) {
        final VertxSliceServer server = new VertxSliceServer(
            this.vertx,
            new BaseSlice(metrics, slice),
            sport
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
    private static Metrics metrics(final Settings settings) {
        return Optional.ofNullable(settings.meta())
            .map(meta -> meta.yamlMapping("metrics"))
            .<Metrics>map(root -> new MetricsFromConfig(root).metrics())
            .orElse(NopMetrics.INSTANCE);
    }
}

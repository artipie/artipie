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

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.http.ArtipieRepositories;
import com.artipie.http.BaseSlice;
import com.artipie.http.MainSlice;
import com.artipie.http.Slice;
import com.artipie.metrics.Metrics;
import com.artipie.metrics.MetricsFromConfig;
import com.artipie.metrics.nop.NopMetrics;
import com.artipie.repo.ConfigFile;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Vertx server entry point.
 * @since 1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ExecutableStatementCountCheck (500 lines)
 */
@SuppressWarnings("PMD.PrematureDeclaration")
public final class VertxMain {

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
     * @param config Config file path.
     * @param vertx The Vert.x instance.
     * @param port HTTP port
     */
    public VertxMain(final Path config, final Vertx vertx, final int port) {
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
        final Settings settings = this.settings(this.config);
        final Metrics metrics = metrics(settings);
        final int main = this.listenOn(new MainSlice(settings), metrics, this.port);
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
     * @throws IOException If fails
     * @throws ParseException If fails
     */
    public static void main(final String... args) throws IOException, ParseException {
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
        new VertxMain(config, vertx, port).start();
    }

    /**
     * Find artipie settings.
     * @param path Settings path
     * @return Settings instance
     * @throws IOException On read error
     * @todo #284:30min Extract this method to separate class and write proper unit tests
     *  for that. Also add tests for `JavaResource` class which is used to copy resources.
     */
    private Settings settings(final Path path) throws IOException {
        boolean initialize = Boolean.parseBoolean(System.getenv("ARTIPIE_INIT"));
        if (!Files.exists(path)) {
            new JavaResource("example/artipie.yaml").copy(path);
            initialize = true;
        }
        final Settings settings = new YamlSettings(
            Yaml.createYamlInput(path.toFile()).readYamlMapping()
        );
        final BlockingStorage bsto = new BlockingStorage(settings.storage());
        final Key init = new Key.From(".artipie", "initialized");
        if (initialize && !bsto.exists(init)) {
            final List<String> resources = Arrays.asList(
                "_credentials.yaml", StorageAliases.FILE_NAME, "_permissions.yaml"
            );
            for (final String res : resources) {
                final Path tmp = Files.createTempFile(res, ".tmp");
                new JavaResource(String.format("example/repo/%s", res)).copy(tmp);
                bsto.save(new Key.From(res), Files.readAllBytes(tmp));
                Files.delete(tmp);
            }
            bsto.save(init, "true".getBytes());
            Logger.info(
                VertxMain.class,
                String.join(
                    "\n",
                    "", "", "\t+===============================================================+",
                    "\t\t\t\t\tHello!",
                    "\t\tArtipie configuration was not found, created default.",
                    "\t\t\tDefault username/password: `artipie`/`artipie`. ",
                    "\t\t\t\t   Check the dashboard at:",
                    String.format(
                        "\t\t\thttp://localhost:%d/dashboard/artipie",
                        this.port
                    ),
                    "\t-===============================================================-", ""
                )
            );
        }
        return settings;
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
                .map(name -> new RepositoriesFromStorage(storage).config(name))
                .map(stage -> stage.toCompletableFuture().join())
                .collect(Collectors.toList())
        ).toCompletableFuture().join();
        for (final RepoConfig repo : configs) {
            try {
                repo.port().ifPresent(
                    prt -> {
                        final String name = new ConfigFile(repo.name()).name();
                        this.listenOn(
                            new ArtipieRepositories(settings).slice(new Key.From(name), true),
                            metrics, prt
                        );
                        Logger.info(
                            VertxMain.class,
                            "Artipie repo '%s' was started on port %d", name, prt
                        );
                    }
                );
            } catch (final IllegalStateException err) {
                Logger.error(
                    this,
                    "Invalid repo config file %s: %[exception]s", repo.name(),
                    err
                );
            }
        }
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

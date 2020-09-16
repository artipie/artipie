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
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.http.ArtipieRepositories;
import com.artipie.http.Pie;
import com.artipie.http.Slice;
import com.artipie.http.TrafficMetricSlice;
import com.artipie.metrics.Metrics;
import com.artipie.metrics.MetricsFromConfig;
import com.artipie.metrics.PrefixedMetrics;
import com.artipie.metrics.nop.NopMetrics;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.javatuples.Pair;

/**
 * Vertx server entry point.
 * @since 1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ExecutableStatementCountCheck (500 lines)
 * @checkstyle AvoidInlineConditionalsCheck (500 lines)
 * @since 1.0
 */
@SuppressWarnings({
    "PMD.PrematureDeclaration", "PMD.SimplifyStartsWith", "PMD.AvoidDuplicateLiterals"
})
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
     * Server.
     */
    private VertxSliceServer server;

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
    }

    /**
     * Starts the server.
     *
     * @return Port the servers listening on.
     * @throws IOException In case of error reading settings.
     * @throws InterruptedException If interrupted.
     */
    public int start() throws IOException, InterruptedException {
        final Settings settings = settings(this.config);
        final Metrics metrics = metrics(settings);
        this.server = new VertxSliceServer(
            this.vertx,
            new TrafficMetricSlice(
                new ResponseMetricsSlice(
                    new Pie(settings),
                    new PrefixedMetrics(metrics, "http.response.")
                ),
                new PrefixedMetrics(metrics, "http.")
            ),
            this.port
        );
        this.findAndStartOnSpecificPorts(settings);
        final int prt = this.server.start();
        Logger.info(VertxMain.class, "Artipie was started on port %d", prt);
        return prt;
    }

    /**
     * Stops server releasing all resources.
     */
    public void stop() {
        Optional.ofNullable(this.server).ifPresent(VertxSliceServer::stop);
    }

    /**
     * Entry point.
     * @param args CLI args
     * @throws IOException If fails
     * @throws ParseException If fails
     * @throws InterruptedException If interrupted.
     */
    public static void main(final String... args)
        throws IOException, ParseException, InterruptedException {
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
     * Find repos and start them on specific port.
     * @param settings The settings
     * @throws IOException In case of error reading settings.
     * @throws InterruptedException If interrupted.
     */
    private void findAndStartOnSpecificPorts(final Settings settings)
        throws IOException, InterruptedException {
        final ArtipieRepositories repositories = new ArtipieRepositories(settings);
        final BlockingStorage blocking = new BlockingStorage(settings.storage());
        new ArrayList<>(blocking.list(Key.ROOT))
            .stream()
            .filter(
                key -> {
                    final String full = key.string();
                    final int index = full.lastIndexOf('/');
                    final String fname = index == -1 ? full : full.substring(index);
                    return !fname.startsWith("_")
                        && (fname.endsWith(".yml") || fname.endsWith(".yaml"));
                }
            )
            .map(
                key -> {
                    final int sport;
                    try {
                        sport = Yaml.createYamlInput(new String(blocking.value(key)))
                            .readYamlMapping()
                            .yamlMapping("repo")
                            .integer("port");
                        return new Pair<>(sport, key);
                    } catch (final InterruptedException | IOException exc) {
                        throw new IllegalStateException(exc);
                    }
                }
            ).filter(pair -> pair.getValue0() > 0)
            .forEach(
                pair -> {
                    try {
                        final Integer sport = pair.getValue0();
                        final Key key = new Key.From(
                            pair.getValue1().string()
                                .replaceAll("\\.yaml", "")
                                .replaceAll("\\.yml", "")
                        );
                        final Slice slice = repositories.slice(key);
                        new VertxSliceServer(this.vertx, slice, sport).start();
                        Logger.info(
                            VertxMain.class,
                            "Slice at %s started at port:%d",
                            key.string(),
                            sport
                        );
                    } catch (final IOException exc) {
                        throw new IllegalStateException(exc);
                    }
                }
            );
    }

    /**
     * Find artipie settings.
     * @param path Settings path
     * @return Settings instance
     * @throws IOException On read error
     * @todo #284:30min Extract this method to separate class and write proper unit tests
     *  for that. Also add tests for `JavaResource` class which is used to copy resources.
     */
    private static Settings settings(final Path path) throws IOException {
        if (!Files.exists(path)) {
            new JavaResource("example/artipie.yaml").copy(path);
            Files.createDirectory(Paths.get("./repo"));
            final List<String> resources = Arrays.asList(
                "_credentials.yaml", "_storages.yaml", "_permissions.yaml"
            );
            for (final String res : resources) {
                new JavaResource(String.format("example/repo/%s", res))
                    .copy(Paths.get(String.format("./repo/%s", res)));
            }
            Logger.info(
                VertxMain.class,
                String.join(
                    " ",
                    "Settings were not found, creating default.",
                    "Default username/password: `artipie`/`artipie`. ",
                    "Check the dashboard at http://localhost/dashboard/artipie"
                )
            );
        }
        return new YamlSettings(Files.readString(path, StandardCharsets.UTF_8));
    }

    /**
     * Creates and initialize metrics from settings.
     *
     * @param settings Settings.
     * @return Metrics.
     * @throws IOException In case of I/O error reading settings.
     */
    private static Metrics metrics(final Settings settings) throws IOException {
        return Optional.ofNullable(settings.meta())
            .map(meta -> meta.yamlMapping("metrics"))
            .<Metrics>map(root -> new MetricsFromConfig(root).metrics())
            .orElse(NopMetrics.INSTANCE);
    }
}

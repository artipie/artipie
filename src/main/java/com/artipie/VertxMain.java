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

import com.artipie.http.Pie;
import com.artipie.http.Slice;
import com.artipie.metrics.Metrics;
import com.artipie.metrics.MetricsFromConfig;
import com.artipie.metrics.PrefixedMetrics;
import com.artipie.metrics.memory.MetricsLogPublisher;
import com.artipie.metrics.nop.NopMetrics;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.LoggerFactory;

/**
 * Vertx server entry point.
 * @since 1.0
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.PrematureDeclaration")
public final class VertxMain implements Runnable {

    /**
     * The Vert.x instance.
     */
    private final Vertx vertx;

    /**
     * Slice to serve.
     */
    private final Slice slice;

    /**
     * Server port.
     */
    private final int port;

    /**
     * Ctor.
     * @param slice To server
     * @param vertx The Vert.x instance.
     * @param port HTTP port
     */
    private VertxMain(final Slice slice, final Vertx vertx, final int port) {
        this.slice = slice;
        this.vertx = vertx;
        this.port = port;
    }

    @Override
    public void run() {
        new VertxSliceServer(this.vertx, this.slice, this.port).start();
    }

    /**
     * Entry point.
     * @param args CLI args
     * @throws IOException If fails
     * @throws ParseException If fails
     */
    public static void main(final String... args) throws IOException, ParseException {
        final Vertx vertx = Vertx.vertx();
        final String storage;
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
            storage = cmd.getOptionValue(fopt);
        } else {
            throw new IllegalStateException("Storage is not configured");
        }
        final Settings settings = settings(Path.of(storage));
        new VertxMain(
            new ResponseMetricsSlice(
                new Pie(settings), new PrefixedMetrics(metrics(settings), "http.response.")
            ),
            vertx,
            port
        ).run();
        Logger.info(VertxMain.class, "Artipie was started on port %d", port);
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
                    "Check the dashboard at http://localhost/artipie"
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
            .<Metrics>map(
                root -> {
                    final MetricsFromConfig metrics = new MetricsFromConfig(root);
                    new MetricsLogPublisher(
                        LoggerFactory.getLogger(Metrics.class),
                        metrics.metrics(),
                        metrics.interval()
                    ).start();
                    return metrics.metrics();
                }
            ).orElse(NopMetrics.INSTANCE);
    }
}

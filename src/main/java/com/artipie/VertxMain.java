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

import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.reactivex.Completable;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

/**
 * Vertx server entry point.
 * @since 1.0
 */
@SuppressWarnings({"PMD.PrematureDeclaration", "PMD.AvoidDuplicateLiterals", "deprecation"})
public final class VertxMain implements Verticle {

    /**
     * The Vert.x instance.
     */
    private Vertx vrtx;

    /**
     * Slice server.
     */
    private VertxSliceServer server;

    /**
     * Entry point.
     * @param args CLI args
     * @throws Exception If fails
     * @checkstyle ExecutableStatementCountCheck (30 lines)
     */
    public static void main(final String... args) throws Exception {
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
        final Context ctx = vertx.getOrCreateContext();
        final JsonObject config = ctx.config();
        config.put("storage", storage);
        config.put("port", port);
        final VertxMain main = new VertxMain();
        main.init(vertx, ctx);
        main.start(
            Future.future(
                event -> {
                }
            )
        );
        Logger.info(VertxMain.class, "Artipie was started on port %d", port);
    }

    @Override
    public Vertx getVertx() {
        return this.vrtx;
    }

    @Override
    public void init(final Vertx vertx, final Context context) {
        this.vrtx = vertx;
        final JsonObject config = context.config();
        try {
            this.server = new VertxSliceServer(
                io.vertx.reactivex.core.Vertx.newInstance(this.vrtx),
                new Pie(
                    new YamlSettings(
                        Files.readString(
                            Path.of(config.getString("storage")), Charset.defaultCharset()
                        )
                    ),
                    io.vertx.reactivex.core.Vertx.newInstance(this.vrtx)
                ),
                config.getInteger("port")
            );
        } catch (final IOException iex) {
            throw new UncheckedIOException(iex);
        }
    }

    @Override
    public void start(final Future<Void> future) {
        Completable.fromAction(this.server::start)
            .subscribe(future::complete, future::fail);
    }

    @Override
    public void stop(final Future<Void> future) {
        Completable.fromAction(this.server::stop)
            .subscribe(future::complete, future::fail);
    }
}

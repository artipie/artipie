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

import com.artipie.http.Slice;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Vertx server entry point.
 * @since 1.0
 */
public final class VertxMain implements Runnable {

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
     * @param port HTTP port
     */
    private VertxMain(final Slice slice, final int port) {
        this.slice = slice;
        this.port = port;
    }

    @Override
    public void run() {
        try (VertxSliceServer srv = new VertxSliceServer(this.slice, this.port)) {
            srv.start();
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // @checkstyle MagicNumberCheck (1 line)
                    Thread.sleep(100);
                } catch (final InterruptedException iox) {
                    Logger.info(this, "interrupted: %s", iox);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Entry point.
     * @param args CLI args
     * @throws IOException If fails
     * @throws ParseException If fails
     */
    public static void main(final String... args) throws IOException, ParseException {
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
        new VertxMain(
            new Pie(
                new YamlSettings(
                    Files.readString(Path.of(storage), Charset.defaultCharset())
                )
            ),
            port
        ).run();
    }
}

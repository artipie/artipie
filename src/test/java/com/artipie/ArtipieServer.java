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
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Artipie server with single repository configured.
 *
 * @since 0.10
 */
public class ArtipieServer {

    /**
     * Root path.
     */
    private final Path root;

    /**
     * Repo name.
     */
    private final String name;

    /**
     * Repo config.
     */
    private final String config;

    /**
     * Vert.x instance used to run the server.
     */
    private Vertx vertx;

    /**
     * Running server.
     */
    private VertxMain server;

    /**
     * Ctor.
     *
     * @param root Root directory.
     * @param name Repo name.
     * @param config Repo config.
     */
    public ArtipieServer(final Path root, final String name, final String config) {
        this.root = root;
        this.name = name;
        this.config = config;
    }

    /**
     * Starts the server.
     *
     * @return Port the servers listening on.
     * @throws IOException In case of error creating configs or running the server.
     */
    public int start() throws IOException {
        final Path repos = this.root.resolve("repos");
        repos.toFile().mkdir();
        Files.write(
            repos.resolve(String.format("%s.yaml", this.name)),
            this.config.getBytes()
        );
        final Path cfg = this.root.resolve("artipie.yaml");
        Files.write(
            cfg,
            Yaml.createYamlMappingBuilder().add(
                "meta",
                Yaml.createYamlMappingBuilder()
                    .add(
                        "storage",
                        Yaml.createYamlMappingBuilder()
                            .add("type", "fs")
                            .add("path", repos.toString())
                            .build()
                    )
                    .add("layout", "flat")
                    .build()
            ).toString().getBytes()
        );
        this.vertx = Vertx.vertx();
        this.server = new VertxMain(cfg, this.vertx, 0);
        return this.server.start();
    }

    /**
     * Stops server releasing all resources.
     */
    public void stop() {
        Optional.ofNullable(this.server).ifPresent(VertxMain::stop);
        Optional.ofNullable(this.vertx).ifPresent(Vertx::close);
    }
}

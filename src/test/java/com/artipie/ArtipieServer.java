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
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/**
 * Artipie server with single repository configured.
 *
 * @since 0.10
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class ArtipieServer {

    /**
     * User Alice.
     */
    public static final User ALICE = new User("alice", "123");

    /**
     * User Bob.
     */
    public static final User BOB = new User("bob", "qwerty");

    /**
     * User Carol.
     */
    public static final User CAROL = new User("carol", "LetMeIn");

    /**
     * Credentials file name.
     */
    public static final String CREDENTIALS_FILE = "_credentials.yml";

    /**
     * All users.
     */
    private static final Collection<User> USERS = Arrays.asList(
        ArtipieServer.ALICE, ArtipieServer.BOB, ArtipieServer.CAROL
    );

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
     * Free port for starting server. If equal to 0, the server
     * starts on an arbitrary free port.
     */
    private final int freeport;

    /**
     * Vert.x instance used to run the server.
     */
    private Vertx vertx;

    /**
     * Running server.
     */
    private VertxMain server;

    /**
     * Port server is listening.
     */
    private int prt;

    /**
     * Artipie layout.
     */
    private final String layout;

    /**
     * Ctor.
     *
     * @param root Root directory.
     * @param name Repo name.
     * @param config Repo config.
     */
    public ArtipieServer(final Path root, final String name, final String config) {
        this(root, name, config, 0);
    }

    /**
     * Ctor.
     *
     * @param root Root directory.
     * @param name Repo name.
     * @param config Repo config.
     */
    public ArtipieServer(final Path root, final String name, final RepoConfigYaml config) {
        this(root, name, config.toString(), 0);
    }

    /**
     * Ctor.
     *
     * @param root Root directory.
     * @param name Repo name.
     * @param config Repo config.
     * @param port Free port.
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    public ArtipieServer(final Path root, final String name, final String config,
        final int port) {
        this(root, name, config, port, "flat");
    }

    /**
     * Ctor.
     *
     * @param root Root directory.
     * @param name Repo name.
     * @param config Repo config.
     * @param layout Layout
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    public ArtipieServer(final Path root, final String name, final RepoConfigYaml config,
        final String layout) {
        this(root, name, config.toString(), 0, layout);
    }

    /**
     * Ctor.
     *
     * @param root Root directory.
     * @param name Repo name.
     * @param config Repo config.
     * @param port Free port.
     * @param layout Layout
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    public ArtipieServer(final Path root, final String name, final String config,
        final int port, final String layout) {
        this.root = root;
        this.name = name;
        this.config = config;
        this.freeport = port;
        this.layout = layout;
    }

    /**
     * Starts the server.
     *
     * @return Port the servers listening on.
     * @throws IOException In case of error creating configs or running the server.
     * @todo #449:30min Extract class for building settings in YAML format.
     *  Building of settings YAML for usage in tests occurs more the once.
     *  It is used to build setting in `ArtipieServer`
     *  and in `YamlSettingsTest` to create settings examples in unit tests.
     *  Some examples in `YamlSettingsTest` are just string constants.
     *  It would be nice to extract a class for building settings in YAML format
     *  for usage in all these places.
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
                    .add(
                        "credentials",
                        Yaml.createYamlMappingBuilder()
                            .add("type", "file")
                            .add("path", ArtipieServer.CREDENTIALS_FILE)
                            .build()
                    )
                    .add("layout", this.layout)
                    .add("base_url", "http://artipie.example.com")
                    .build()
            ).build().toString().getBytes()
        );
        Files.write(
            repos.resolve(ArtipieServer.CREDENTIALS_FILE),
            credentials().getBytes()
        );
        this.vertx = Vertx.vertx();
        this.server = new VertxMain(cfg, this.vertx, this.freeport);
        this.prt = this.server.start();
        return this.prt;
    }

    /**
     * Stops server releasing all resources.
     */
    public void stop() {
        Optional.ofNullable(this.server).ifPresent(VertxMain::stop);
        Optional.ofNullable(this.vertx).ifPresent(Vertx::close);
    }

    /**
     * Port server is listening.
     *
     * @return Server port, 0 if server is not started.
     */
    public int port() {
        return this.prt;
    }

    /**
     * Create credentials YAML with known users.
     *
     * @return Credentials YAML.
     */
    private static String credentials() {
        final CredsConfigYaml cred = new CredsConfigYaml();
        for (final User user : ArtipieServer.USERS) {
            cred.withUserAndPlainPswd(user.name(), user.password());
        }
        return cred.toString();
    }

    /**
     * User with name and password.
     *
     * @since 0.10
     */
    public static final class User {

        /**
         * Username.
         */
        private final String username;

        /**
         * Password.
         */
        private final String pwd;

        /**
         * Ctor.
         *
         * @param username Username.
         * @param pwd Password.
         */
        public User(final String username, final String pwd) {
            this.username = username;
            this.pwd = pwd;
        }

        /**
         * Get username.
         *
         * @return Username.
         */
        public String name() {
            return this.username;
        }

        /**
         * Get password.
         *
         * @return Password.
         */
        public String password() {
            return this.pwd;
        }
    }
}

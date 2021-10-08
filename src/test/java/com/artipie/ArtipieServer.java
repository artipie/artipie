/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.artipie.asto.Key;
import com.artipie.http.client.jetty.JettyClientSlices;
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
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.AvoidCatchingGenericException"})
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
    public static final String CREDENTIALS_FILE = "_credentials.yaml";

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
     * Repo configs key.
     */
    private final Optional<Key> repoconfigs;

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
        this(root, name, config, port, "flat", Optional.empty());
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
        this(root, name, config.toString(), 0, layout, Optional.empty());
    }

    /**
     * Ctor.
     *
     * @param root Root directory.
     * @param name Repo name.
     * @param config Repo config.
     * @param repoconfigs Repo configs key
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    public ArtipieServer(final Path root, final String name, final RepoConfigYaml config,
        final Optional<Key> repoconfigs) {
        this(root, name, config.toString(), 0, "flat", repoconfigs);
    }

    /**
     * Ctor.
     *
     * @param root Root directory.
     * @param name Repo name.
     * @param config Repo config.
     * @param port Free port.
     * @param layout Layout
     * @param repoconfig Repository configs key
     * @checkstyle ParameterNumberCheck (2 lines)
     */
    public ArtipieServer(final Path root, final String name, final String config,
        final int port, final String layout, final Optional<Key> repoconfig) {
        this.root = root;
        this.name = name;
        this.config = config;
        this.freeport = port;
        this.layout = layout;
        this.repoconfigs = repoconfig;
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
        this.repoconfigs.ifPresent(key -> repos.resolve(key.string()).toFile().mkdir());
        Files.write(
            repos.resolve(
                String.format(
                    "%s%s.yaml",
                    this.repoconfigs.map(Key::string)
                        .map(key -> String.format("%s/", key)).orElse(""),
                    this.name
                )
            ),
            this.config.getBytes()
        );
        final Path cfg = this.root.resolve("artipie.yaml");
        YamlMappingBuilder meta = Yaml.createYamlMappingBuilder()
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
            .add("base_url", "http://artipie.example.com");
        if (this.repoconfigs.isPresent()) {
            meta = meta.add("repo_configs", this.repoconfigs.get().string());
        }
        Files.write(
            cfg,
            Yaml.createYamlMappingBuilder().add("meta", meta.build()).build().toString().getBytes()
        );
        Files.write(
            repos.resolve(ArtipieServer.CREDENTIALS_FILE),
            credentials().getBytes()
        );
        final JettyClientSlices http = new JettyClientSlices(new HttpClientSettings());
        try {
            http.start();
            // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Exception err) {
            throw new IllegalStateException(err);
        }
        this.vertx = Vertx.vertx();
        this.server = new VertxMain(http, cfg, this.vertx, this.freeport);
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

        /**
         * Username and password separated by colon.
         * @return Username:pswd
         */
        public String nameAndPswd() {
            return String.format("%s:%s", this.username, this.pwd);
        }
    }
}

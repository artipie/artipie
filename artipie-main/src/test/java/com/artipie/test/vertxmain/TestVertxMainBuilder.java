/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.test.vertxmain;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import com.artipie.VertxMain;
import com.artipie.asto.test.TestResource;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Artipie server builder.
 */
public class TestVertxMainBuilder {

    public static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a file storage config with {@code dir} path
     *
     * @param dir Storage path.
     * @return Config yaml.
     */
    public static YamlMapping fileStorageCfg(Path dir) {
        return Yaml.createYamlMappingBuilder()
                .add("type", "fs")
                .add("path", dir.toAbsolutePath().toString())
                .build();
    }

    private static Path createDirIfNotExists(Path dir) {
        Objects.requireNonNull(dir, "Directory cannot be null");
        try {
            return Files.exists(dir) ? dir : Files.createDirectory(dir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final Path base;
    private final Path repos;
    private final Path security;
    private final Path users;
    private final Path roles;

    /**
     * Creates Artipie server builder.
     *
     * @param base Work directory
     */
    public TestVertxMainBuilder(Path base) {
        this.base = createDirIfNotExists(base);
        this.repos = createDirIfNotExists(this.base.resolve("repo"));
        this.security = createDirIfNotExists(this.base.resolve("security"));
        this.users = createDirIfNotExists(this.security.resolve("users"));
        this.roles = createDirIfNotExists(this.security.resolve("roles"));
    }

    /**
     * Copies the user's security file to the server's work directory.
     *
     * @param name   Username
     * @param source Path to a user's security file
     * @return TestVertxMainBuilder
     * @throws IOException If the copy operation is failed.
     */
    public TestVertxMainBuilder withUser(String name, String source) throws IOException {
        Files.copy(
                new TestResource(source).asPath(),
                this.users.resolve(name + ".yaml")
        );
        return this;
    }

    /**
     * Copies the role's security file to the server's work directory.
     *
     * @param name   Role name
     * @param source Path to a role's security file
     * @return TestVertxMainBuilder
     * @throws IOException If the copy operation is failed.
     */
    public TestVertxMainBuilder withRole(String name, String source) throws IOException {
        Files.copy(
                new TestResource(source).asPath(),
                this.roles.resolve(name + ".yaml")
        );
        return this;
    }

    /**
     * Copies the repo config file to the server's work directory.
     *
     * @param name   Repository name
     * @param source Path to a repo config file
     * @return TestVertxMainBuilder
     * @throws IOException If the copy operation failed
     */
    public TestVertxMainBuilder withRepo(String name, String source) throws IOException {
        Files.copy(
                new TestResource(source).asPath(),
                this.repos.resolve(name + ".yml")
        );
        return this;
    }

    /**
     * Creates a docker repository config file in the server's work directory.
     *
     * @param name Repository name
     * @param data Repository data path
     * @return TestVertxMainBuilder
     * @throws IOException If the create operation failed
     */
    public TestVertxMainBuilder withDockerRepo(String name, Path data) throws IOException {
        saveRepoConfig(name,
                Yaml.createYamlMappingBuilder()
                        .add("type", "docker")
                        .add("storage", fileStorageCfg(data))
                        .build()
        );
        return this;
    }

    /**
     * Creates a docker repository config file in the server's work directory.
     *
     * @param name Repository name
     * @param port Repository http server port
     * @param data Repository data path
     * @return TestVertxMainBuilder
     * @throws IOException If the create operation failed
     */
    public TestVertxMainBuilder withDockerRepo(String name, int port, Path data) throws IOException {
        saveRepoConfig(name,
                Yaml.createYamlMappingBuilder()
                        .add("type", "docker")
                        .add("port", String.valueOf(port))
                        .add("storage", fileStorageCfg(data))
                        .build()
        );
        return this;
    }

    /**
     * Creates a docker-proxy repository config file in the server's work directory.
     *
     * @param name    Repository name
     * @param data    Repository data path
     * @param remotes Remotes registry urls
     * @return TestVertxMainBuilder
     * @throws IOException If the create operation failed
     */
    public TestVertxMainBuilder withDockerProxyRepo(String name, Path data, URI... remotes) throws IOException {
        Assertions.assertNotEquals(0, remotes.length, "Empty remotes");
        YamlSequenceBuilder seq = Yaml.createYamlSequenceBuilder();
        for (URI url : remotes) {
            seq = seq.add(
                    Yaml.createYamlMappingBuilder().add("url", url.toString()).build()
            );
        }
        saveRepoConfig(name,
                Yaml.createYamlMappingBuilder()
                        .add("type", "docker-proxy")
                        .add("remotes", seq.build())
                        .add("storage", fileStorageCfg(data))
                        .build()
        );
        return this;
    }

    /**
     * Builds and starts Artipie server.
     *
     * @return TestVertxMain
     * @throws IOException If failed
     */
    public TestVertxMain build() throws IOException {
        return build(freePort());
    }

    /**
     * Builds and starts Artipie server.
     *
     * @param port Artipie http server port
     * @return TestVertxMain
     * @throws IOException If failed
     */
    public TestVertxMain build(int port) throws IOException {
        Path cfg = new MetaBuilder()
                .withRepoDir(this.repos)
                .withSecurityDir(this.security)
                .build(this.base);
        VertxMain server = new VertxMain(cfg, port);
        Assertions.assertEquals(port, server.start(freePort()));

        return new TestVertxMain(port, server);
    }

    private void saveRepoConfig(String name, YamlMapping cfg) throws IOException {
        byte[] repo = Yaml.createYamlMappingBuilder()
                .add("repo", cfg)
                .build().toString().getBytes();
        Files.write(this.repos.resolve(name + ".yml"), repo);
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.test;

import com.artipie.rpm.misc.UncheckedConsumer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Junit extension which provides latest artipie server container and client container.
 * Artipie container can be accessed from client container by {@code artipie} hostname.
 * To run a command in client container and match a result use {@code assertExec} method.
 * @since 0.19
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public final class TestDeployment implements BeforeEachCallback, AfterEachCallback {

    /**
     * Default name of the ClientContainer.
     */
    private static final String DEF = "artipie";

    /**
     * Artipie builder.
     */
    private final Map<String, Supplier<ArtipieContainer>> asup;

    /**
     * Client builder.
     */
    private final Supplier<ClientContainer> csup;

    /**
     * Container network.
     */
    private Network net;

    /**
     * Artipie container.
     */
    private Map<String, ArtipieContainer> artipie;

    /**
     * Client container.
     */
    private ClientContainer client;

    /**
     * Artipie loggers.
     */
    private final ConcurrentMap<String, Consumer<OutputFrame>> aloggers;

    /**
     * Client logger.
     */
    private final Consumer<OutputFrame> clilogger;

    /**
     * New container test.
     * @param artipie Artipie container definition
     * @param client Client container definition
     */
    public TestDeployment(final Supplier<ArtipieContainer> artipie,
        final Supplier<ClientContainer> client) {
        this(new MapOf<>(new MapEntry<>(TestDeployment.DEF, artipie)), client);
    }

    /**
     * New container test.
     * @param artipie Artipie container definition
     * @param client Client container definition
     */
    public TestDeployment(final Map<String, Supplier<ArtipieContainer>> artipie,
        final Supplier<ClientContainer> client) {
        this.asup = artipie;
        this.csup = client;
        this.clilogger = TestDeployment.slfjLog(TestDeployment.ClientContainer.class, "client");
        this.aloggers = new ConcurrentHashMap<>();
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        this.net = Network.newNetwork();
        this.artipie = this.asup.entrySet().stream()
            .map(entry -> new MapEntry<>(entry.getKey(), entry.getValue().get()))
            .map(
                entry -> new MapEntry<>(
                    entry.getKey(),
                    entry.getValue()
                        .withNetwork(this.net).withNetworkAliases(entry.getKey())
                        .withLogConsumer(
                            this.aloggers.computeIfAbsent(
                                entry.getKey(),
                                name -> TestDeployment.slfjLog(
                                    TestDeployment.ArtipieContainer.class, entry.getKey()
                                )
                            )
                        )
                )
            ).collect(Collectors.toMap(MapEntry::getKey, MapEntry::getValue));
        this.client = this.csup.get()
            .withNetwork(this.net)
            .withLogConsumer(this.clilogger)
            .withCommand("tail", "-f", "/dev/null");
        this.artipie.values().forEach(GenericContainer::start);
        this.client.start();
        this.client.execInContainer("sleep", "3");
        this.artipie.values().forEach(
            new UncheckedConsumer<>(cnt -> cnt.execInContainer("sleep", "3"))
        );
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        if (this.artipie != null) {
            this.artipie.values().forEach(GenericContainer::stop);
            this.artipie.values().forEach(Startable::close);
        }
        if (this.client != null) {
            this.client.stop();
            this.client.close();
        }
        if (this.net != null) {
            this.net.close();
        }
    }

    /**
     * Assert binary file in Artipie container using matcher provided.
     * @param name Artipie container name
     * @param msg Assertion message
     * @param path Path in container
     * @param matcher Matcher InputStream of content
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public void assertArtipieContent(final String name, final String msg, final String path,
        final Matcher<byte[]> matcher) {
        this.artipie.get(name).copyFileFromContainer(
            path, stream -> {
                MatcherAssert.assertThat(msg, IOUtils.toByteArray(stream), matcher);
                return null;
            }
        );
    }

    /**
     * Assert binary file in Artipie container using matcher provided.
     * @param msg Assertion message
     * @param path Path in container
     * @param matcher Matcher InputStream of content
     */
    public void assertArtipieContent(final String msg, final String path,
        final Matcher<byte[]> matcher) {
        this.assertArtipieContent(TestDeployment.DEF, msg, path, matcher);
    }

    /**
     * Exec command in client container and assert result.
     * @param msg Assertion message on failure
     * @param matcher Exec result matcher
     * @param cmd Commands to execute
     * @throws IOException In case of client exception
     */
    public void assertExec(final String msg, final Matcher<ExecResult> matcher,
        final String... cmd) throws IOException {
        final ExecResult exec;
        try {
            exec = this.client.execInContainer(cmd);
        } catch (final InterruptedException ignore) {
            Thread.currentThread().interrupt();
            return;
        }
        MatcherAssert.assertThat(msg, exec, matcher);
    }

    /**
     * Exec command in client container and assert result.
     * @param msg Assertion message on failure
     * @param matcher Exec result matcher
     * @param cmd Command list to execute
     * @throws IOException In case of client exception
     */
    public void assertExec(final String msg, final Matcher<ExecResult> matcher,
        final List<String> cmd) throws IOException {
        this.assertExec(msg, matcher, cmd.toArray(new String[0]));
    }

    /**
     * Just exec command in client container and fail on error.
     * @param cmd Command list to execute
     * @throws IOException In case of client exception
     */
    public void clientExec(final String... cmd) throws IOException {
        this.assertExec(
            String.format("Failed to run `%s`", String.join(" ", cmd)),
            new ContainerResultMatcher(),
            cmd
        );
    }

    /**
     * Put binary data into Artipie container.
     * @param name Artipie container name
     * @param bin Data to put
     * @param path Path in the container
     */
    public void putBinaryToArtipie(final String name, final byte[] bin, final String path) {
        this.artipie.get(name).copyFileToContainer(Transferable.of(bin), path);
    }

    /**
     * Put binary data into Artipie container.
     * @param bin Data to put
     * @param path Path in the container
     */
    public void putBinaryToArtipie(final byte[] bin, final String path) {
        this.putBinaryToArtipie(TestDeployment.DEF, bin, path);
    }

    /**
     * Put resource to artipie server.
     * @param name Artipie container name
     * @param res Resource path
     * @param path Artipie path
     */
    public void putResourceToArtipie(final String name, final String res, final String path) {
        this.artipie.get(name).copyFileToContainer(
            MountableFile.forClasspathResource(res), path
        );
    }

    /**
     * Put resource to artipie server.
     * @param res Resource path
     * @param path Artipie path
     */
    public void putResourceToArtipie(final String res, final String path) {
        this.putResourceToArtipie(TestDeployment.DEF, res, path);
    }

    /**
     * Put binary data into client container.
     * @param bin Data to put
     * @param path Path in the container
     */
    public void putBinaryToClient(final byte[] bin, final String path) {
        this.client.copyFileToContainer(Transferable.of(bin), path);
    }

    /**
     * Sets up client environment for docker tests.
     * @throws IOException On error
     */
    public void setUpForDockerTests() throws IOException {
        // @checkstyle MethodBodyCommentsCheck (10 lines)
        // @checkstyle LineLengthCheck (10 lines)
        this.clientExec("apk", "add", "--update", "--no-cache", "openrc", "docker");
        // needs this command to initialize openrc directories on first call
        this.clientExec("rc-status");
        // this flag file is needed to tell openrc working in non-boot mode
        this.clientExec("touch", "/run/openrc/softlevel");
        // allow artipie:8080 insecure connection before starting docker daemon
        this.clientExec("sed", "-i", "s/DOCKER_OPTS=\"\"/DOCKER_OPTS=\"--insecure-registry=artipie:8080\"/g", "/etc/conf.d/docker");
        this.clientExec("rc-service", "docker", "start");
        // docker daemon needs some time to start after previous command
        this.clientExec("sleep", "3");
    }

    /**
     * Returns a logger named corresponding to the class passed as parameter.
     * @param clazz Class type of container
     * @param prefix Prefix for output logs
     * @return Logger.
     */
    private static Consumer<OutputFrame> slfjLog(final Class<?> clazz, final String prefix) {
        return new Slf4jLogConsumer(
            LoggerFactory.getLogger(clazz)
        ).withPrefix(prefix);
    }

    /**
     * Artipie container builder.
     * @since 0.18
     */
    public static final class ArtipieContainer extends GenericContainer<ArtipieContainer> {

        /**
         * New default artipie container.
         */
        public ArtipieContainer() {
            this(DockerImageName.parse("artipie/artipie:1.0-SNAPSHOT"));
        }

        /**
         * New artipie container with image name.
         *
         * @param name Image name
         */
        public ArtipieContainer(final DockerImageName name) {
            super(name);
        }

        /**
         * New defaut definition.
         *
         * @return Default container definition
         */
        @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
        public static ArtipieContainer defaultDefinition() {
            return new ArtipieContainer().withConfig("artipie.yaml");
        }

        /**
         * With artipie config file.
         *
         * @param res Config resource name
         * @return Self
         */
        public ArtipieContainer withConfig(final String res) {
            return this.withClasspathResourceMapping(
                res, "/etc/artipie/artipie.yml", BindMode.READ_ONLY
            );
        }

        /**
         * With artipie config.
         *
         * @param temp Temp directory
         * @param cfg Config
         * @return Self
         */
        public ArtipieContainer withConfig(
            final Path temp,
            final String cfg
        ) {
            return this.withFileSystemBind(
                write(temp, cfg, "artipie"),
                "/etc/artipie/artipie.yml",
                BindMode.READ_ONLY
            );
        }

        /**
         * With repository configuration.
         *
         * @param temp Temp directory
         * @param config Repo config
         * @param repo Repository name
         * @return Self
         */
        public ArtipieContainer withRepoConfig(
            final Path temp,
            final String config,
            final String repo
        ) {
            return this.withFileSystemBind(
                write(temp, config, repo),
                String.format("/var/artipie/repo/%s.yaml", repo),
                BindMode.READ_ONLY
            );
        }

        /**
         * With repository configuration.
         *
         * @param res Config resource path
         * @param repo Repository name
         * @return Self
         */
        public ArtipieContainer withRepoConfig(final String res, final String repo) {
            return this.withClasspathResourceMapping(
                res, String.format("/var/artipie/repo/%s.yaml", repo), BindMode.READ_ONLY
            );
        }

        /**
         * With credentials.
         *
         * @param temp Temp directory
         * @param cred Credentials
         * @return Self
         */
        public ArtipieContainer withCredentials(
            final Path temp,
            final String cred
        ) {
            return this.withFileSystemBind(
                write(temp, cred, "_credentials.yaml"),
                "/var/artipie/repo/_credentials.yaml",
                BindMode.READ_ONLY
            );
        }

        /**
         * With credentials.
         *
         * @param res Credentials resource path
         * @return Self
         */
        public ArtipieContainer withCredentials(final String res) {
            return this.withClasspathResourceMapping(
                res, "/var/artipie/repo/_credentials.yaml", BindMode.READ_ONLY
            );
        }

        /**
         * With repository permissions.
         *
         * @param temp Temp directory
         * @param perms Permissions
         * @return Self
         */
        public ArtipieContainer withPermissions(
            final Path temp,
            final String perms
        ) {
            return this.withFileSystemBind(
                write(temp, perms, "_permissions.yaml"),
                "/var/artipie/repo/_permissions.yaml",
                BindMode.READ_ONLY
            );
        }

        /**
         * With repository permissions.
         *
         * @param res Config resource path
         * @return Self
         */
        public ArtipieContainer withPermissions(final String res) {
            return this.withClasspathResourceMapping(
                res, "/var/artipie/repo/_permissions.yaml", BindMode.READ_ONLY
            );
        }

        /**
         * Write yaml to a file in the temp directory.
         *
         * @param temp Temp directory
         * @param yaml Yaml content
         * @param name File name prefix
         * @return Path to the yaml resource
         */
        private static String write(
            final Path temp,
            final String yaml,
            final String name
        ) {
            try {
                final Path res = temp.resolve(
                    String.format("%s%d.yaml", name, yaml.hashCode())
                );
                if (res.toFile().exists()) {
                    Files.delete(res);
                }
                return Files.write(
                    res,
                    yaml.getBytes()
                ).toString();
            } catch (final IOException err) {
                throw new UncheckedIOException(err);
            }
        }
    }

    /**
     * Client container builder.
     * @since 0.19
     */
    public static final class ClientContainer extends GenericContainer<ClientContainer> {

        /**
         * New client container with name.
         * @param name Image name
         */
        public ClientContainer(final String name) {
            super(DockerImageName.parse(name));
        }
    }
}

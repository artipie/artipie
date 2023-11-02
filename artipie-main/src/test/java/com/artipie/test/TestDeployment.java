/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.test;

import com.artipie.rpm.misc.UncheckedConsumer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import org.hamcrest.core.IsAnything;
import org.hamcrest.core.IsEqual;
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
import org.testcontainers.images.builder.ImageFromDockerfile;
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
     * Get binary file from Artipie container.
     * @param path Path in container
     * @return Binary data
     */
    public byte[] getArtipieContent(final String path) {
        return this.artipie.get(TestDeployment.DEF).copyFileFromContainer(
            path, IOUtils::toByteArray
        );
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
     * Exec command in client container.
     * @param cmd Command list to execute
     * @return Command execution result
     * @throws IOException In case of client exception
     */
    public ExecResult exec(final String... cmd) throws IOException {
        final ExecResult exec;
        try {
            exec = this.client.execInContainer(cmd);
        } catch (final InterruptedException err) {
            Thread.currentThread().interrupt();
            throw new IOException(err);
        }
        return exec;
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
     * Put classpath resource into client container.
     * @param res Classpath resource
     * @param path Path in container
     */
    public void putClasspathResourceToClient(final String res, final String path) {
        final MountableFile file = MountableFile.forClasspathResource(res);
        this.client.copyFileToContainer(file, path);
    }

    /**
     * Sets up client environment for docker tests.
     * @throws IOException On error
     */
    public void setUpForDockerTests() throws IOException {
        // @checkstyle MagicNumberCheck (2 lines)
        this.setUpForDockerTests(8080);
    }

    /**
     * Sets up client environment for docker tests.
     *
     * @param ports Artipie port
     * @throws IOException On error
     */
    public void setUpForDockerTests(final int... ports) throws IOException {
        // @checkstyle MethodBodyCommentsCheck (18 lines)
        // @checkstyle LineLengthCheck (10 lines)
        this.clientExec("apk", "add", "--update", "--no-cache", "openrc", "docker");
        // needs this command to initialize openrc directories on first call
        this.clientExec("rc-status");
        // this flag file is needed to tell openrc working in non-boot mode
        this.clientExec("touch", "/run/openrc/softlevel");
        // allow artipie:8080 insecure connection before starting docker daemon
        final StringBuilder sbl = new StringBuilder(30);
        for (final int port : ports) {
            sbl.append("--insecure-registry=artipie:").append(port).append(' ');
        }
        this.clientExec(
            "sed", "-i",
            String.format("s/DOCKER_OPTS=\"\"/DOCKER_OPTS=\"%s\"/g", sbl),
            "/etc/conf.d/docker"
        );
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
         * New default definition.
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
         * With user.
         *
         * @param res User resource path
         * @param uname Username
         * @return Self
         */
        public ArtipieContainer withUser(final String res, final String uname) {
            return this.withClasspathResourceMapping(
                res, String.format("/var/artipie/security/users/%s.yaml", uname), BindMode.READ_ONLY
            );
        }

        /**
         * With role.
         *
         * @param res User resource path
         * @param rname Role name
         * @return Self
         */
        public ArtipieContainer withRole(final String res, final String rname) {
            return this.withClasspathResourceMapping(
                res, String.format("/var/artipie/security/roles/%s.yaml", rname), BindMode.READ_ONLY
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
         *
         * @param name Image name
         */
        public ClientContainer(final String name) {
            super(DockerImageName.parse(name));
        }

        /**
         * New client container with custom image.
         *
         * @param image Custom docker image
         */
        public ClientContainer(final ImageFromDockerfile image) {
            super(image);
        }
    }

    /**
     * Docker test builder.
     *
     * @since 0.24
     */
    public static final class DockerTest {
        /**
         * Deployment for tests.
         */
        private final TestDeployment deployment;

        /**
         * Registry.
         */
        private final String registry;

        /**
         * List of test commands.
         */
        private final List<DockerCommand> commands;

        /**
         * Ctor.
         *
         * @param deployment Deployment for tests
         * @param registry Registry
         */
        public DockerTest(final TestDeployment deployment, final String registry) {
            this(deployment, registry, new ArrayList<>(0));
        }

        /**
         * Ctor.
         *
         * @param deployment Deployment for tests
         * @param registry Registry
         * @param commands List of test commands
         */
        public DockerTest(
            final TestDeployment deployment,
            final String registry,
            final List<DockerCommand> commands
        ) {
            this.deployment = deployment;
            this.registry = registry;
            this.commands = commands;
        }

        /**
         * Exec command and assert result.
         *
         * @throws IOException In case of exception
         */
        public void assertExec() throws IOException {
            for (final DockerCommand cmd : this.commands) {
                cmd.assertExec();
            }
        }

        /**
         * Login to Artipie as the user with name 'alice'.
         *
         * @return Self
         */
        public DockerTest loginAsAlice() {
            return this.login("alice", "123");
        }

        /**
         * Login to Artipie.
         *
         * @param user User name
         * @param pwd Password
         * @return Self
         */
        public DockerTest login(final String user, final String pwd) {
            this.commands.add(
                new DockerCommand(
                    this.deployment,
                    "Failed to login to Artipie",
                    List.of(
                        "docker", "login",
                        "--username", user,
                        "--password", pwd,
                        this.registry
                    ),
                    new ContainerResultMatcher()
                )
            );
            return this;
        }

        /**
         * Pull image.
         *
         * @param img Image
         * @return Self
         */
        public DockerTest pull(final String img) {
            return this.pull(img, new IsAnything<>());
        }

        /**
         * Pull image.
         *
         * @param img Image
         * @param stdout Expected message in stdout
         * @return Self
         */
        public DockerTest pull(final String img, final Matcher<String> stdout) {
            this.commands.add(
                new DockerCommand(
                    this.deployment,
                    String.format("Failed to pull image [image=%s]", img),
                    List.of("docker", "pull", img),
                    new ContainerResultMatcher(
                        new IsEqual<>(ContainerResultMatcher.SUCCESS),
                        stdout
                    )
                )
            );
            return this;
        }

        /**
         * Push image.
         *
         * @param img Image
         * @return Self
         */
        public DockerTest push(final String img) {
            return this.push(img, new IsAnything<>());
        }

        /**
         * Push image.
         *
         * @param img Image.
         * @param stdout Expected message in stdout
         * @return Self
         */
        public DockerTest push(final String img, final Matcher<String> stdout) {
            this.commands.add(
                new DockerCommand(
                    this.deployment,
                    String.format(
                        "Failed to push image to Artipie [image=%s]", img
                    ),
                    List.of("docker", "push", img),
                    new ContainerResultMatcher(
                        new IsEqual<>(ContainerResultMatcher.SUCCESS),
                        stdout
                    )
                )
            );
            return this;
        }

        /**
         * Create a tag {@code target} that refers to {@code source}.
         *
         * @param source Source image
         * @param target Target image
         * @return Self
         */
        public DockerTest tag(final String source, final String target) {
            this.commands.add(
                new DockerCommand(
                    this.deployment,
                    String.format(
                        "Failed to tag image [source=%s, target=%s]",
                        source, target
                    ),
                    List.of("docker", "tag", source, target),
                    new ContainerResultMatcher()
                )
            );
            return this;
        }

        /**
         * Remove image.
         *
         * @param img Image
         * @return Self
         */
        public DockerTest remove(final String img) {
            return this.remove(img, new IsAnything<>());
        }

        /**
         * Remove image.
         *
         * @param img Image
         * @param stdout Expected message in stdout
         * @return Self
         */
        public DockerTest remove(final String img, final Matcher<String> stdout) {
            this.commands.add(
                new DockerCommand(
                    this.deployment,
                    String.format(
                        "Failed to remove image from Artipie [image=%s]", img
                    ),
                    List.of("docker", "image", "rm", img),
                    new ContainerResultMatcher(
                        new IsEqual<>(ContainerResultMatcher.SUCCESS),
                        stdout
                    )
                )
            );
            return this;
        }
    }

    /**
     * Docker test command.
     *
     * @since 0.24
     */
    static final class DockerCommand {
        /**
         * Deployment for tests.
         */
        private final TestDeployment deployment;

        /**
         * Assertion message on failure.
         */
        private final String msg;

        /**
         * Command list to execute.
         */
        private final List<String> cmd;

        /**
         * Exec result matcher.
         */
        private final Matcher<ExecResult> matcher;

        /**
         * Ctor.
         *
         * @param deployment Deployment for tests
         * @param msg Assertion message on failure
         * @param cmd Command list to execute
         * @param matcher Exec result matcher
         * @checkstyle ParameterNumberCheck (2 lines)
         */
        DockerCommand(
            final TestDeployment deployment,
            final String msg,
            final List<String> cmd,
            final Matcher<ExecResult> matcher
        ) {
            this.deployment = deployment;
            this.msg = msg;
            this.cmd = cmd;
            this.matcher = matcher;
        }

        /**
         * Exec command and assert result.
         *
         * @throws IOException In case of exception
         */
        void assertExec() throws IOException {
            this.deployment.assertExec(this.msg, this.matcher, this.cmd);
        }
    }
}

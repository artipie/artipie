/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.test;

import java.io.IOException;
import java.util.Map;
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
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Junit extension which provides latest artipie server container and client container.
 * Artipie container can be accessed from client container by {@code artipie} hostname.
 * To run a command in client container and match a result use {@code assertExec} method.
 * @since 0.19
 * @todo #855:30min Add Slf4j logging consumer for artipie container
 *  It's not working for some reason and prints nothing to the test output.
 *  A workaround was added with custom consumer for system stdout frame printing as
 *  lambda. Properly configure SLf4j consumer and remove this workaround.
 */
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
    }

    @Override
    @SuppressWarnings("PMD.SystemPrintln")
    public void beforeEach(final ExtensionContext context) throws Exception {
        this.net = Network.newNetwork();
        this.artipie = this.asup.entrySet().stream().map(
            entry -> new MapEntry<>(
                entry.getKey(),
                entry.getValue().get().withNetwork(this.net).withNetworkAliases(entry.getKey())
            )
        ).collect(Collectors.toMap(MapEntry::getKey, MapEntry::getValue));
        this.artipie = this.artipie.entrySet().stream().map(
            entry -> new MapEntry<>(
                entry.getKey(),
                entry.getValue().withLogConsumer(
                    frame -> System.out.printf(
                        "%s - %s: %s", entry.getValue().getDockerImageName(), entry.getKey(),
                        frame.getUtf8String()
                    )
                )
            )
        ).collect(Collectors.toMap(MapEntry::getKey, MapEntry::getValue));
        this.client = this.csup.get()
            .withNetwork(this.net)
            .withCommand("tail", "-f", "/dev/null");
        this.artipie.values().forEach(GenericContainer::start);
        this.client.start();
    }

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
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
     * @param cmd Command to execute
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
         * @param name Image name
         */
        public ArtipieContainer(final DockerImageName name) {
            super(name);
        }

        /**
         * With artipie config file.
         * @param res Config resource name
         * @return Self
         */
        public ArtipieContainer withConfig(final String res) {
            return this.withClasspathResourceMapping(
                res, "/etc/artipie/artipie.yml", BindMode.READ_ONLY
            );
        }

        /**
         * With repository configuration.
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
         * With repository configuration.
         * @param res Config resource path
         * @return Self
         */
        public ArtipieContainer withCredentials(final String res) {
            return this.withClasspathResourceMapping(
                res, "/var/artipie/repo/_credentials.yaml", BindMode.READ_ONLY
            );
        }

        /**
         * New defaut definition.
         * @return Default container definition
         */
        @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
        public static ArtipieContainer defaultDefinition() {
            return new ArtipieContainer().withConfig("artipie.yaml");
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

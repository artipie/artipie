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

package com.artipie.test;

import java.io.IOException;
import java.util.function.Supplier;
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
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Junit extension which provides latest artipie server container and client container.
 * Artipie container can be accessed from client container by {@code artipie} hostname.
 * To run a command in client container and match a result use {@code assertExec} method.
 * @since 0.19
 */
public final class TestDeployment implements BeforeEachCallback, AfterEachCallback {

    /**
     * Artipie builder.
     */
    private final Supplier<ArtipieContainer> asup;

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
    private ArtipieContainer artipie;

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
        this.asup = artipie;
        this.csup = client;
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        this.net = Network.newNetwork();
        this.artipie = this.asup.get()
            .withNetwork(this.net).withNetworkAliases("artipie");
        this.artipie = this.artipie.withLogConsumer(
            new Slf4jLogConsumer(LoggerFactory.getLogger(this.artipie.getDockerImageName()))
        );
        this.client = this.csup.get()
            .withNetwork(this.net)
            .withCommand("tail", "-f", "/dev/null");
        this.artipie.start();
        this.client.start();
    }

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        if (this.artipie != null) {
            this.artipie.stop();
            this.artipie.close();
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
     * Put resource to artipie server.
     * @param res Resource path
     * @param path Artipie path
     */
    public void putResourceArtipie(final String res, final String path) {
        this.artipie.copyFileToContainer(
            MountableFile.forClasspathResource(res), path
        );
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

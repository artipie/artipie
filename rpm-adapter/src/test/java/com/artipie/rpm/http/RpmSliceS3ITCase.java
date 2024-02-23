/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.http;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.amazonaws.services.s3.AmazonS3;
import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StoragesLoader;
import com.artipie.http.auth.Authentication;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.rpm.Digest;
import com.artipie.rpm.NamingPolicy;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.Rpm;
import com.artipie.rpm.TestRpm;
import com.artipie.security.policy.Policy;
import com.artipie.security.policy.PolicyByUsername;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Test for {@link RpmSlice}, uses dnf and yum rpm-package managers,
 * checks that list and install works with and without authentication.
 * @since 0.10
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
@Disabled("https://github.com/artipie/artipie/issues/1409")
public final class RpmSliceS3ITCase {

    @RegisterExtension
    static final S3MockExtension MOCK = S3MockExtension.builder()
        .withSecureConnection(false)
        .build();

    /**
     * Bucket to use in tests.
     */
    private String bucket;

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Installed packages verifier.
     */
    private static final ListOf<String> INSTALLED = new ListOf<>(
        "Installed", "time-1.7-45.el7.x86_64", "Complete!"
    );

    /**
     * Packaged list verifier.
     */
    private static final ListOf<String> AVAILABLE = new ListOf<>(
        "Available Packages", "time.x86_64", "1.7-45.el7"
    );

    /**
     * Temporary directory for all tests.
     */
    @TempDir
    Path tmp;

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Testing storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp(final AmazonS3 client) {
        this.bucket = UUID.randomUUID().toString();
        client.createBucket(this.bucket);
        this.storage = new StoragesLoader()
            .newObject(
                "s3",
                new Config.YamlStorageConfig(
                    Yaml.createYamlMappingBuilder()
                        .add("region", "us-east-1")
                        .add("bucket", this.bucket)
                        .add("endpoint", String.format("http://localhost:%d", MOCK.getHttpPort()))
                        .add(
                            "credentials",
                            Yaml.createYamlMappingBuilder()
                                .add("type", "basic")
                                .add("accessKeyId", "foo")
                                .add("secretAccessKey", "bar")
                                .build()
                        )
                        .build()
                )
            );
    }

    @ParameterizedTest
    @CsvSource({
        "redhat/ubi9:9.0.0,yum,repo-pkgs",
        "fedora:36,dnf,repository-packages"
    })
    void canListAndInstallFromArtipieRepo(final String linux,
        final String mngr, final String rey) throws Exception {
        this.start(Policy.FREE, Authentication.ANONYMOUS, "", linux);
        MatcherAssert.assertThat(
            "Lists 'time' package",
            this.exec(mngr, rey, "list"),
            new StringContainsInOrder(RpmSliceS3ITCase.AVAILABLE)
        );
        MatcherAssert.assertThat(
            "Installs 'time' package",
            this.exec(mngr, rey, "install"),
            new StringContainsInOrder(RpmSliceS3ITCase.INSTALLED)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "redhat/ubi9:9.0.0,yum,repo-pkgs",
        "fedora:36,dnf,repository-packages"
    })
    void canListAndInstallFromArtipieRepoWithAuth(final String linux,
        final String mngr, final String key) throws Exception {
        final String mark = "mark";
        final String pswd = "abc";
        this.start(
            new PolicyByUsername(mark),
            new Authentication.Single(mark, pswd),
            String.format("%s:%s@", mark, pswd),
            linux
        );
        MatcherAssert.assertThat(
            "Lists 'time' package",
            this.exec(mngr, key, "list"),
            new StringContainsInOrder(RpmSliceS3ITCase.AVAILABLE)
        );
        MatcherAssert.assertThat(
            "Installs 'time' package",
            this.exec(mngr, key, "install"),
            new StringContainsInOrder(RpmSliceS3ITCase.INSTALLED)
        );
    }

    @AfterEach
    void stopContainer() {
        this.server.close();
        this.cntn.stop();
    }

    @AfterAll
    static void close() {
        RpmSliceS3ITCase.VERTX.close();
    }

    /**
     * Executes yum command in container.
     * @param mngr Rpm manager
     * @param key Key to specify repo
     * @param action What to do
     * @return String stdout
     * @throws Exception On error
     */
    private String exec(final String mngr, final String key, final String action) throws Exception {
        final Container.ExecResult res = this.cntn.execInContainer(
            mngr, "-y", key, "example", action
        );
        Logger.info(this, res.toString());
        return res.getStdout();
    }

    /**
     * Starts VertxSliceServer and docker container.
     * @param policy Permissions
     * @param auth Authentication
     * @param cred String with user name and password to add in url, uname:pswd@
     * @param linux Linux distribution name and version
     * @throws Exception On error
     */
    private void start(final Policy<?> policy, final Authentication auth, final String cred,
        final String linux) throws Exception {
        new TestRpm.Time().put(this.storage);
        final RepoConfig config = new RepoConfig.Simple(
            Digest.SHA256, new NamingPolicy.HashPrefixed(Digest.SHA1), true
        );
        new Rpm(this.storage, config).batchUpdate(Key.ROOT).blockingAwait();
        this.server = new VertxSliceServer(
            RpmSliceS3ITCase.VERTX,
            new LoggingSlice(new RpmSlice(this.storage, policy, auth, config))
        );
        final int port = this.server.start();
        Testcontainers.exposeHostPorts(port);
        final Path setting = this.tmp.resolve("example.repo");
        this.tmp.resolve("example.repo").toFile().createNewFile();
        Files.write(
            setting,
            new ListOf<>(
                "[example]",
                "name=Example Repository",
                String.format("baseurl=http://%shost.testcontainers.internal:%d/", cred, port),
                "enabled=1",
                "gpgcheck=0"
            )
        );
        final Path product = this.tmp.resolve("product-id.conf");
        this.tmp.resolve("product-id.conf").toFile().createNewFile();
        Files.write(
            product,
            new ListOf<>(
                "[main]",
                "enabled=0"
            )
        );
        final Path mng = this.tmp.resolve("subscription-manager.conf");
        this.tmp.resolve("subscription-manager.conf").toFile().createNewFile();
        Files.write(
            mng,
            new ListOf<>(
                "[main]",
                "enabled=0"
            )
        );
        this.cntn = new GenericContainer<>(linux)
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
        this.cntn.execInContainer("mv", "/home/example.repo", "/etc/yum.repos.d/");
        this.cntn.execInContainer("mv", "/home/product-id.conf", "/etc/yum/pluginconf.d/product-id.conf");
        this.cntn.execInContainer("mv", "/home/subscription-manager.conf", "/etc/yum/pluginconf.d/subscription-manager.conf");
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.debian.http.DebianSlice;
import com.artipie.http.RsStatus;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.security.policy.Policy;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.MatchesPattern;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Test for {@link DebianSlice} with GPG-signature.
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
public final class DebianGpgSliceITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Temporary directory for all tests.
     */
    @TempDir
    Path tmp;

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Vertx server port.
     */
    private int port;

    @BeforeEach
    void init() throws Exception {
        this.storage = new InMemoryStorage();
        new TestResource("public-key.asc").saveTo(new FileStorage(this.tmp));
        final Storage settings = new InMemoryStorage();
        final String key = "secret-keys.gpg";
        new TestResource(key).saveTo(settings);
        this.server = new VertxSliceServer(
            DebianGpgSliceITCase.VERTX,
            new LoggingSlice(
                new DebianSlice(
                    this.storage,
                    Policy.FREE,
                    (username, password) -> Optional.empty(),
                    new Config.FromYaml(
                        "artipie",
                        Yaml.createYamlMappingBuilder()
                            .add("Components", "main")
                            .add("Architectures", "amd64")
                            .add("gpg_password", "1q2w3e4r5t6y7u")
                            .add("gpg_secret_key", key)
                            .build(),
                        settings
                    ), Optional.empty()
                )
            )
        );
        this.port = this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        Files.write(
            this.tmp.resolve("sources.list"),
            String.format(
                "deb http://host.testcontainers.internal:%d/ artipie main", this.port
            ).getBytes()
        );
        this.cntn = new GenericContainer<>("artipie/deb-tests:1.0")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
        this.exec("apt-key", "add", "/home/public-key.asc");
        this.exec("mv", "/home/sources.list", "/etc/apt/");
    }

    @Test
    void putAndInstallWithInReleaseFileWorks() throws Exception {
        final HttpURLConnection con = (HttpURLConnection) URI.create(
            String.format("http://localhost:%d/main/aglfn_1.7-3_amd64.deb", this.port)
        ).toURL().openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("PUT");
        final DataOutputStream out = new DataOutputStream(con.getOutputStream());
        out.write(new TestResource("aglfn_1.7-3_amd64.deb").asBytes());
        out.close();
        MatcherAssert.assertThat(
            "Response for upload is OK",
            con.getResponseCode(),
            new IsEqual<>(RsStatus.OK.code())
        );
        con.disconnect();
        MatcherAssert.assertThat(
            "InRelease file is used on update the world",
            this.exec("apt-get", "update"),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    new MatchesPattern(Pattern.compile("[\\S\\s]*Get:1 http://host.testcontainers.internal:\\d+ artipie InRelease[\\S\\s]*")),
                    new MatchesPattern(Pattern.compile("[\\S\\s]*Get:2 http://host.testcontainers.internal:\\d+ artipie/main amd64 Packages \\[685 B][\\S\\s]*")),
                    new IsNot<>(new StringContains("Get:3"))
                )
            )
        );
        MatcherAssert.assertThat(
            "Package was downloaded and unpacked",
            this.exec("apt-get", "install", "-y", "aglfn"),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    new MatchesPattern(Pattern.compile("[\\S\\s]*Get:1 http://host.testcontainers.internal:\\d+ artipie/main amd64 aglfn amd64 1.7-3 \\[29.9 kB][\\S\\s]*")),
                    new IsNot<>(new StringContains("Get:2")),
                    new StringContainsInOrder(new ListOf<>("Unpacking aglfn", "Setting up aglfn"))
                )
            )
        );
    }

    /**
     * Current apt-get version uses InRelease index if it is present in the repo and ignores
     * Release and Release.gpg files. Release and Release.gpg can be required by some older clients,
     * apt-get uses these files if InRelease is absent. We generate Release, Release.gpg and
     * InRelease in {@link com.artipie.debian.http.ReleaseSlice}, so to make this test work
     * it is necessary to remove InRelease index before calling apt-get.
     * @throws Exception On error
     */
    @Test
    void installWithReleaseFileWorks() throws Exception {
        this.copyPackage("aglfn_1.7-3_amd64.deb");
        final HttpURLConnection con = (HttpURLConnection) URI.create(
            String.format("http://localhost:%d/main/aglfn_1.7-3_amd64.deb", this.port)
        ).toURL().openConnection();
        con.setRequestMethod("GET");
        con.getResponseCode();
        con.disconnect();
        this.storage.delete(new Key.From("dists", "artipie", "InRelease")).join();
        MatcherAssert.assertThat(
            "Release file is used on update the world",
            this.exec("apt-get", "update"),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    new MatchesPattern(Pattern.compile("[\\S\\s]*Get:2 http://host.testcontainers.internal:\\d+ artipie Release[\\S\\s]*")),
                    new MatchesPattern(Pattern.compile("[\\S\\s]*Get:3 http://host.testcontainers.internal:\\d+ artipie Release.gpg[\\S\\s]*")),
                    new MatchesPattern(Pattern.compile("[\\S\\s]*Get:4 http://host.testcontainers.internal:\\d+ artipie/main amd64 Packages \\[1351 B][\\S\\s]*")),
                    new IsNot<>(new StringContains("Get:5"))
                )
            )
        );
        MatcherAssert.assertThat(
            "Package was downloaded and unpacked",
            this.exec("apt-get", "install", "-y", "aglfn"),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    new MatchesPattern(Pattern.compile("[\\S\\s]*Get:1 http://host.testcontainers.internal:\\d+ artipie/main amd64 aglfn amd64 1.7-3 \\[29.9 kB][\\S\\s]*")),
                    new IsNot<>(new StringContains("Get:2")),
                    new StringContainsInOrder(new ListOf<>("Unpacking aglfn", "Setting up aglfn"))
                )
            )
        );
    }

    @AfterEach
    void stop() {
        this.server.stop();
        this.cntn.stop();
    }

    private void copyPackage(final String pkg) {
        new TestResource(pkg).saveTo(this.storage, new Key.From("main", pkg));
        new TestResource("Packages.gz")
            .saveTo(this.storage, new Key.From("dists/artipie/main/binary-amd64/Packages.gz"));
    }

    private String exec(final String... command) throws Exception {
        final Container.ExecResult res = this.cntn.execInContainer(command);
        Logger.debug(this, "Command:\n%s\nResult:\n%s", String.join(" ", command), res.toString());
        return res.getStdout();
    }
}

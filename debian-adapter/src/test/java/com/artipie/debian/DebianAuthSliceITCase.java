/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.debian.http.DebianSlice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.RsStatus;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.perms.EmptyPermissions;
import com.artipie.security.policy.Policy;
import com.artipie.security.policy.PolicyByUsername;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PermissionCollection;
import java.util.Optional;
import org.apache.commons.codec.binary.Base64;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

/**
 * Test for {@link DebianSlice}.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
public final class DebianAuthSliceITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * User name.
     */
    private static final String USER = "alice";

    /**
     * Password.
     */
    private static final String PSWD = "123";

    /**
     * Auth.
     */
    private static final String AUTH = String.format(
        "%s:%s", DebianAuthSliceITCase.USER, DebianAuthSliceITCase.PSWD
    );

    /**
     * Temporary directory for all tests.
     */
    @TempDir
    Path tmp;

    /**
     * Artipie port.
     */
    private int port;

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    @Test
    void pushAndInstallWorks() throws Exception {
        this.init(Policy.FREE);
        final HttpURLConnection con = this.getConnection(DebianAuthSliceITCase.AUTH, "PUT");
        final DataOutputStream out = new DataOutputStream(con.getOutputStream());
        out.write(new TestResource("aglfn_1.7-3_amd64.deb").asBytes());
        out.close();
        MatcherAssert.assertThat(
            "Response for upload is OK",
            con.getResponseCode(),
            new IsEqual<>(RsStatus.OK.code())
        );
        this.cntn.execInContainer("apt-get", "update");
        final Container.ExecResult res =
            this.cntn.execInContainer("apt-get", "install", "-y", "aglfn");
        MatcherAssert.assertThat(
            "Package was downloaded and unpacked",
            res.getStdout(),
            new StringContainsInOrder(new ListOf<>("Unpacking aglfn", "Setting up aglfn"))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT"})
    void returnsUnauthorizedWhenUserIsUnknown(final String method) throws Exception {
        this.init(new PolicyByUsername(DebianAuthSliceITCase.USER));
        MatcherAssert.assertThat(
            "Response is UNAUTHORIZED",
            this.getConnection("mark:abc", method).getResponseCode(),
            new IsEqual<>(RsStatus.UNAUTHORIZED.code())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT"})
    void returnsForbiddenWhenOperationIsNotAllowed(final String method) throws Exception {
        this.init(
            user -> {
                final PermissionCollection res;
                if (DebianAuthSliceITCase.USER.equals(user.name())) {
                    final AdapterBasicPermission perm =
                        new AdapterBasicPermission("artipie", Action.NONE);
                    res = perm.newPermissionCollection();
                    res.add(perm);
                } else {
                    res = EmptyPermissions.INSTANCE;
                }
                return res;
            }
        );
        MatcherAssert.assertThat(
            "Response is FORBIDDEN",
            this.getConnection(DebianAuthSliceITCase.AUTH, method).getResponseCode(),
            new IsEqual<>(RsStatus.FORBIDDEN.code())
        );
    }

    @AfterEach
    void stop() {
        this.server.stop();
        this.cntn.stop();
    }

    private HttpURLConnection getConnection(final String auth, final String method)
        throws IOException {
        final HttpURLConnection con = (HttpURLConnection) URI.create(
            String.format("http://localhost:%d/main/aglfn_1.7-3_amd64.deb", this.port)
        ).toURL().openConnection();
        con.setDoOutput(true);
        con.addRequestProperty(
            "Authorization",
            String.format(
                "Basic %s",
                new String(
                    Base64.encodeBase64(auth.getBytes(StandardCharsets.UTF_8))
                )
            )
        );
        con.setRequestMethod(method);
        return con;
    }

    private void init(final Policy<?> permissions) throws IOException, InterruptedException {
        final Storage storage = new InMemoryStorage();
        this.server = new VertxSliceServer(
            DebianAuthSliceITCase.VERTX,
            new LoggingSlice(
                new DebianSlice(
                    storage,
                    permissions,
                    new Authentication.Single(
                        DebianAuthSliceITCase.USER, DebianAuthSliceITCase.PSWD
                    ),
                    new Config.FromYaml(
                        "artipie",
                        Yaml.createYamlMappingBuilder()
                            .add("Components", "main")
                            .add("Architectures", "amd64")
                            .build(),
                        storage
                    ),
                    Optional.empty()
                )
            )
        );
        this.port = this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        final Path setting = this.tmp.resolve("sources.list");
        Files.write(
            setting,
            String.format(
                "deb [trusted=yes] http://%s@host.testcontainers.internal:%d/ artipie main",
                DebianAuthSliceITCase.AUTH, this.port
            ).getBytes()
        );
        this.cntn = new GenericContainer<>("debian:11")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
        this.cntn.execInContainer("mv", "/home/sources.list", "/etc/apt/");
        this.cntn.execInContainer("ls", "-la", "/etc/apt/");
        this.cntn.execInContainer("cat", "/etc/apt/sources.list");
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conan.http;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import com.amazonaws.services.s3.AmazonS3;
import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StoragesLoader;
import com.artipie.asto.test.TestResource;
import com.artipie.conan.ItemTokenizer;
import com.artipie.http.auth.Authentication;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.security.policy.PolicyByUsername;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.core.Vertx;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Tests for {@link ConanSlice}.
 * Test container and data for package base of Ubuntu 20.04 LTS x86_64.
 * @since 0.1
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class ConanSliceS3ITCase {

    @RegisterExtension
    static final S3MockExtension MOCK = S3MockExtension.builder()
        .withSecureConnection(false)
        .build();

    /**
     * Artipie conan username for basic auth.
     */
    public static final String SRV_USERNAME = "demo_login";

    /**
     * Artipie conan password for basic auth.
     */
    public static final String SRV_PASSWORD = "demo_pass";

    /**
     * Test auth token.
     */
    public static final String TOKEN = "demotoken";

    /**
     * Path prefix for conan repository test data.
     */
    private static final String SRV_PREFIX = "conan-test/server_data/data";

    /**
     * Conan server port.
     */
    private static final int CONAN_PORT = 9300;

    /**
     * Conan server zlib package files list for integration tests.
     */
    private static final String[] CONAN_TEST_PKG = {
        "zlib/1.2.11/_/_/0/package/dfbe50feef7f3c6223a476cd5aeadb687084a646/0/conaninfo.txt",
        "zlib/1.2.11/_/_/0/package/dfbe50feef7f3c6223a476cd5aeadb687084a646/0/conan_package.tgz",
        "zlib/1.2.11/_/_/0/package/dfbe50feef7f3c6223a476cd5aeadb687084a646/0/conanmanifest.txt",
        "zlib/1.2.11/_/_/0/package/dfbe50feef7f3c6223a476cd5aeadb687084a646/revisions.txt",
        "zlib/1.2.11/_/_/0/export/conan_export.tgz",
        "zlib/1.2.11/_/_/0/export/conanfile.py",
        "zlib/1.2.11/_/_/0/export/conanmanifest.txt",
        "zlib/1.2.11/_/_/0/export/conan_sources.tgz",
        "zlib/1.2.11/_/_/revisions.txt",
    };

    /**
     * Base dockerfile for test containers.
     */
    private static ImageFromDockerfile base;

    /**
     * Bucket to use in tests.
     */
    private String bucket;

    /**
     * Artipie Storage instance for tests.
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

    @BeforeEach
    void setUp(final AmazonS3 client) throws Exception {
        this.bucket = UUID.randomUUID().toString();
        client.createBucket(this.bucket);
        this.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        this.server.stop();
    }

    @Test
    void conanPathCheck() throws Exception {
        final String stdout = this.cntn.execInContainer("which", "conan").getStdout();
        MatcherAssert.assertThat("`which conan` path must exist", !stdout.isEmpty());
    }

    @Test
    void conanDefaultProfileCheck() throws Exception {
        final Container.ExecResult result = this.cntn.execInContainer(
            "conan", "profile", "show", "default"
        );
        MatcherAssert.assertThat(
            "conan default profile must exist", result.getExitCode() == 0
        );
    }

    @Test
    void conanProfilesCheck() throws Exception {
        final Container.ExecResult result = this.cntn.execInContainer(
            "conan", "profile", "list"
        );
        MatcherAssert.assertThat(
            "conan profiles must work", result.getExitCode() == 0
        );
    }

    @Test
    void conanProfileGenerationCheck() throws Exception {
        Container.ExecResult result = this.cntn.execInContainer(
            "rm", "-rf", "/root/.conan"
        );
        MatcherAssert.assertThat(
            "rm command for old settings must succeed", result.getExitCode() == 0
        );
        result = this.cntn.execInContainer(
            "conan", "profile", "new", "--detect", "default"
        );
        MatcherAssert.assertThat(
            "conan profile generation must succeed", result.getExitCode() == 0
        );
    }

    @Test
    void conanRemotesCheck() throws Exception {
        final Container.ExecResult result = this.cntn.execInContainer(
            "conan", "remote", "list"
        );
        MatcherAssert.assertThat(
            "conan remotes must work", result.getExitCode() == 0
        );
    }

    @Test
    void pingConanServer() throws IOException, InterruptedException {
        final Container.ExecResult result = this.cntn.execInContainer(
            "curl", "--verbose", "--fail", "--show-error",
            "http://host.testcontainers.internal:9300/v1/ping"
        );
        MatcherAssert.assertThat(
            "conan ping must succeed", result.getExitCode() == 0
        );
    }

    @Test
    void conanDownloadPkg() throws IOException, InterruptedException {
        for (final String file : ConanSliceS3ITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceS3ITCase.SRV_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final Container.ExecResult result = this.cntn.execInContainer(
            "conan", "download", "-r", "conan-test", "zlib/1.2.11@"
        );
        MatcherAssert.assertThat(
            "conan download must succeed", result.getExitCode() == 0
        );
    }

    @Test
    void conanDownloadPkgEnvAuthCheck() throws IOException, InterruptedException {
        for (final String file : ConanSliceS3ITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceS3ITCase.SRV_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final Container.ExecResult user = this.cntn.execInContainer(
            "conan", "user", "-c"
        );
        final Container.ExecResult result = this.cntn
            .execInContainer(
                "bash", "-c",
                    String.format(
                    "CONAN_LOGIN_USERNAME=%s CONAN_PASSWORD=%s conan download -r conan-test zlib/1.2.11@",
                    ConanSliceS3ITCase.SRV_USERNAME, ConanSliceS3ITCase.SRV_PASSWORD
                )
            );
        MatcherAssert.assertThat(
            "conan user command must succeed", user.getExitCode() == 0
        );
        MatcherAssert.assertThat(
            "conan download must succeed", result.getExitCode() == 0
        );
    }

    @Test
    void conanDownloadPkgEnvAuthFail() throws IOException, InterruptedException {
        for (final String file : ConanSliceS3ITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceS3ITCase.SRV_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final Container.ExecResult user = this.cntn.execInContainer(
            "conan", "user", "-c"
        );
        final String login = ConanSliceS3ITCase.SRV_USERNAME.substring(
            0, ConanSliceS3ITCase.SRV_USERNAME.length() - 1
        );
        final String password = ConanSliceS3ITCase.SRV_PASSWORD.substring(
            0, ConanSliceS3ITCase.SRV_PASSWORD.length() - 1
        );
        final Container.ExecResult result = this.cntn
            .execInContainer(
                "bash", "-c",
                    String.format(
                    "CONAN_LOGIN_USERNAME=%s CONAN_PASSWORD=%s conan download -r conan-test zlib/1.2.11@",
                    login, password
                )
            );
        MatcherAssert.assertThat(
            "conan user command must succeed", user.getExitCode() == 0
        );
        MatcherAssert.assertThat(
            "conan download must fail", result.getExitCode() != 0
        );
    }

    @Test
    void conanDownloadPkgEnvInvalidLogin() throws IOException, InterruptedException {
        for (final String file : ConanSliceS3ITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceS3ITCase.SRV_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final Container.ExecResult user = this.cntn.execInContainer(
            "conan", "user", "-c"
        );
        final String login = ConanSliceS3ITCase.SRV_USERNAME.substring(
            0, ConanSliceS3ITCase.SRV_USERNAME.length() - 1
        );
        final Container.ExecResult result = this.cntn
            .execInContainer(
                "bash", "-c",
                    String.format(
                    "CONAN_LOGIN_USERNAME=%s CONAN_PASSWORD=%s conan download -r conan-test zlib/1.2.11@",
                    login, ConanSliceS3ITCase.SRV_PASSWORD
                )
            );
        MatcherAssert.assertThat(
            "conan user command must succeed", user.getExitCode() == 0
        );
        MatcherAssert.assertThat(
            "conan download must fail", result.getExitCode() != 0
        );
    }

    @Test
    void conanDownloadPkgEnvInvalidPassword() throws IOException, InterruptedException {
        for (final String file : ConanSliceS3ITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceS3ITCase.SRV_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final Container.ExecResult user = this.cntn.execInContainer(
            "conan", "user", "-c"
        );
        final String password = ConanSliceS3ITCase.SRV_PASSWORD.substring(
            0, ConanSliceS3ITCase.SRV_PASSWORD.length() - 1
        );
        final Container.ExecResult result = this.cntn
            .execInContainer(
                "bash", "-c",
                    String.format(
                    "CONAN_LOGIN_USERNAME=%s CONAN_PASSWORD=%s conan download -r conan-test zlib/1.2.11@",
                    ConanSliceS3ITCase.SRV_USERNAME, password
                )
            );
        MatcherAssert.assertThat(
            "conan user command must succeed", user.getExitCode() == 0
        );
        MatcherAssert.assertThat(
            "conan download must fail", result.getExitCode() != 0
        );
    }

    @Test
    void conanDownloadPkgAsAnonFail() throws IOException, InterruptedException {
        for (final String file : ConanSliceS3ITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceS3ITCase.SRV_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final Container.ExecResult user = this.cntn.execInContainer(
            "conan", "user", "-c"
        );
        final Container.ExecResult result = this.cntn
            .execInContainer(
                "bash", "-c",
                    String.format(
                    "CONAN_LOGIN_USERNAME=%s CONAN_PASSWORD=%s conan download -r conan-test zlib/1.2.11@",
                    "", ""
                )
            );
        MatcherAssert.assertThat(
            "conan user command must succeed", user.getExitCode() == 0
        );
        MatcherAssert.assertThat(
            "conan download must fail", result.getExitCode() != 0
        );
    }

    @Test
    void conanDownloadWrongPkgName() throws IOException, InterruptedException {
        for (final String file : ConanSliceS3ITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceS3ITCase.SRV_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final Container.ExecResult result = this.cntn.execInContainer(
            "conan", "download", "-r", "conan-test", "wronglib/1.2.11@"
        );
        MatcherAssert.assertThat(
            "conan download must exit 1", result.getExitCode() == 1
        );
    }

    @Test
    void conanDownloadWrongPkgVersion() throws IOException, InterruptedException {
        for (final String file : ConanSliceS3ITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceS3ITCase.SRV_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final Container.ExecResult result = this.cntn.execInContainer(
            "conan", "download", "-r", "conan-test", "zlib/1.2.111@"
        );
        MatcherAssert.assertThat(
            "conan download must exit 1", result.getExitCode() == 1
        );
    }

    @Test
    void conanSearchPkg() throws IOException, InterruptedException {
        for (final String file : ConanSliceS3ITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceS3ITCase.SRV_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final Container.ExecResult result = this.cntn.execInContainer(
            "conan", "search", "-r", "conan-test", "zlib/1.2.11@"
        );
        MatcherAssert.assertThat(
            "conan search must succeed", result.getExitCode() == 0
        );
    }

    @Test
    void conanSearchWrongPkgVersion() throws IOException, InterruptedException {
        for (final String file : ConanSliceS3ITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceS3ITCase.SRV_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final Container.ExecResult result = this.cntn.execInContainer(
            "conan", "search", "-r", "conan-test", "zlib/1.2.111@"
        );
        MatcherAssert.assertThat(
            "conan search must exit 1", result.getExitCode() == 1
        );
    }

    @Test
    void conanInstallRecipe() throws IOException, InterruptedException {
        final String arch = this.cntn.execInContainer("uname", "-m").getStdout();
        Assumptions.assumeTrue(arch.startsWith("x86_64"));
        new TestResource(ConanSliceS3ITCase.SRV_PREFIX).addFilesTo(this.storage, Key.ROOT);
        this.cntn.copyFileToContainer(
            Transferable.of(
                Files.readAllBytes(Paths.get("src/test/resources/conan-test/conanfile.txt"))
            ),
            "/w/conanfile.txt"
        );
        final Container.ExecResult result = this.cntn.execInContainer("conan", "install", ".");
        MatcherAssert.assertThat(
            "conan install must succeed", result.getExitCode() == 0
        );
    }

    @Test
    void testPackageUpload() throws IOException, InterruptedException {
        for (final String file : ConanSliceS3ITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceS3ITCase.SRV_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final Container.ExecResult install = this.cntn.execInContainer(
            "conan", "install", "zlib/1.2.11@", "-r", "conan-test"
        );
        final Container.ExecResult upload = this.cntn.execInContainer(
            "conan", "upload", "zlib/1.2.11@", "-r", "conan-test", "--all"
        );
        MatcherAssert.assertThat(
            "conan install must succeed", install.getExitCode() == 0
        );
        MatcherAssert.assertThat(
            "conan upload must succeed", upload.getExitCode() == 0
        );
    }

    @Test
    void testPackageReupload() throws IOException, InterruptedException {
        final Container.ExecResult enable = this.cntn.execInContainer(
            "conan", "remote", "enable", "conancenter"
        );
        final Container.ExecResult instcenter = this.cntn.execInContainer(
            "conan", "install", "zlib/1.2.11@", "-r", "conancenter"
        );
        final Container.ExecResult upload = this.cntn.execInContainer(
            "conan", "upload", "zlib/1.2.11@", "-r", "conan-test", "--all"
        );
        final Container.ExecResult rmcache = this.cntn.execInContainer(
            "rm", "-rfv", "/root/.conan/data"
        );
        final Container.ExecResult disable = this.cntn.execInContainer(
            "conan", "remote", "disable", "conancenter"
        );
        final Container.ExecResult insttest = this.cntn.execInContainer(
            "conan", "install", "zlib/1.2.11@", "-r", "conan-test"
        );
        MatcherAssert.assertThat(
            "conan remote enable must succeed", enable.getExitCode() == 0
        );
        MatcherAssert.assertThat(
            "conan install (conancenter) must succeed", instcenter.getExitCode() == 0
        );
        MatcherAssert.assertThat(
            "conan upload must succeed", upload.getExitCode() == 0
        );
        MatcherAssert.assertThat(
            "rm for conan cache must succeed", rmcache.getExitCode() == 0
        );
        MatcherAssert.assertThat(
            "conan remote disable must succeed", disable.getExitCode() == 0
        );
        MatcherAssert.assertThat(
            "conan install (conan-test) must succeed", insttest.getExitCode() == 0
        );
    }

    /**
     * Starts VertxSliceServer and docker container.
     * @throws Exception On error
     */
    private void start() throws Exception {
        this.storage = StoragesLoader.STORAGES
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
        this.server = new VertxSliceServer(
            new LoggingSlice(
                new ConanSlice(
                    this.storage,
                    new PolicyByUsername(ConanSliceS3ITCase.SRV_USERNAME),
                    new Authentication.Single(
                        ConanSliceS3ITCase.SRV_USERNAME, ConanSliceS3ITCase.SRV_PASSWORD
                    ),
                    new ConanSlice.FakeAuthTokens(ConanSliceS3ITCase.TOKEN, ConanSliceS3ITCase.SRV_USERNAME),
                    new ItemTokenizer(Vertx.vertx()),
                    "test"
            )),
            ConanSliceS3ITCase.CONAN_PORT
        );
        final int port = this.server.start();
        Testcontainers.exposeHostPorts(port);
        this.cntn = new GenericContainer<>("artipie/conan-tests:1.0")
            .withCommand("tail", "-f", "/dev/null")
            .withReuse(true)
            .withAccessToHost(true);
        this.cntn.start();
        this.cntn.execInContainer("conan remote disable conancenter".split(" "));
        this.cntn.execInContainer("conan remote disable conan-center".split(" "));
        this.cntn.execInContainer("bash", "-c", "pwd;ls -lah;env>>/tmp/conan_trace.log");
        this.cntn.execInContainer(
            "conan", "user", "-r", "conan-test", ConanSliceS3ITCase.SRV_USERNAME, "-p", ConanSliceS3ITCase.SRV_PASSWORD
        );
        this.cntn.execInContainer("bash", "-c", "echo 'STARTED'>>/tmp/conan_trace.log");
    }
}

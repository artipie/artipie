/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package  com.artipie.conan.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.conan.ItemTokenizer;
import com.artipie.http.auth.Authentication;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.security.policy.PolicyByUsername;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.core.Vertx;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;

/**
 * Tests for {@link ConanSlice}.
 * Test container and data for package base of Ubuntu 20.04 LTS x86_64.
 * @checkstyle LineLengthCheck (999 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (999 lines)
 * @since 0.1
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class ConanSliceITCase {

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

    static {
        ConanSliceITCase.base = getBaseImage();
    }

    @BeforeEach
    void setUp() throws Exception {
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
        for (final String file : ConanSliceITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceITCase.SRV_PREFIX, file))
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
        for (final String file : ConanSliceITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceITCase.SRV_PREFIX, file))
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
                    ConanSliceITCase.SRV_USERNAME, ConanSliceITCase.SRV_PASSWORD
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
        for (final String file : ConanSliceITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceITCase.SRV_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final Container.ExecResult user = this.cntn.execInContainer(
            "conan", "user", "-c"
        );
        final String login = ConanSliceITCase.SRV_USERNAME.substring(
            0, ConanSliceITCase.SRV_USERNAME.length() - 1
        );
        final String password = ConanSliceITCase.SRV_PASSWORD.substring(
            0, ConanSliceITCase.SRV_PASSWORD.length() - 1
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
        for (final String file : ConanSliceITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceITCase.SRV_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final Container.ExecResult user = this.cntn.execInContainer(
            "conan", "user", "-c"
        );
        final String login = ConanSliceITCase.SRV_USERNAME.substring(
            0, ConanSliceITCase.SRV_USERNAME.length() - 1
        );
        final Container.ExecResult result = this.cntn
            .execInContainer(
                "bash", "-c",
                    String.format(
                    "CONAN_LOGIN_USERNAME=%s CONAN_PASSWORD=%s conan download -r conan-test zlib/1.2.11@",
                    login, ConanSliceITCase.SRV_PASSWORD
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
        for (final String file : ConanSliceITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceITCase.SRV_PREFIX, file))
                .saveTo(this.storage, new Key.From(file));
        }
        final Container.ExecResult user = this.cntn.execInContainer(
            "conan", "user", "-c"
        );
        final String password = ConanSliceITCase.SRV_PASSWORD.substring(
            0, ConanSliceITCase.SRV_PASSWORD.length() - 1
        );
        final Container.ExecResult result = this.cntn
            .execInContainer(
                "bash", "-c",
                    String.format(
                    "CONAN_LOGIN_USERNAME=%s CONAN_PASSWORD=%s conan download -r conan-test zlib/1.2.11@",
                    ConanSliceITCase.SRV_USERNAME, password
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
        for (final String file : ConanSliceITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceITCase.SRV_PREFIX, file))
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
        for (final String file : ConanSliceITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceITCase.SRV_PREFIX, file))
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
        for (final String file : ConanSliceITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceITCase.SRV_PREFIX, file))
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
        for (final String file : ConanSliceITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceITCase.SRV_PREFIX, file))
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
        for (final String file : ConanSliceITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceITCase.SRV_PREFIX, file))
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
        new TestResource(ConanSliceITCase.SRV_PREFIX).addFilesTo(this.storage, Key.ROOT);
        this.cntn.copyFileToContainer(
            Transferable.of(
                Files.readAllBytes(Paths.get("src/test/resources/conan-test/conanfile.txt"))
            ),
            "/home/conanfile.txt"
        );
        final Container.ExecResult result = this.cntn.execInContainer("conan", "install", ".");
        MatcherAssert.assertThat(
            "conan install must succeed", result.getExitCode() == 0
        );
    }

    @Test
    void testPackageUpload() throws IOException, InterruptedException {
        for (final String file : ConanSliceITCase.CONAN_TEST_PKG) {
            new TestResource(String.join("/", ConanSliceITCase.SRV_PREFIX, file))
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
     * @checkstyle ParameterNumberCheck (10 lines)
     */
    private void start() throws Exception {
        this.storage = new InMemoryStorage();
        this.server = new VertxSliceServer(
            new LoggingSlice(
                new ConanSlice(
                    this.storage,
                    new PolicyByUsername(ConanSliceITCase.SRV_USERNAME),
                    new Authentication.Single(
                        ConanSliceITCase.SRV_USERNAME, ConanSliceITCase.SRV_PASSWORD
                    ),
                    new ConanSlice.FakeAuthTokens(ConanSliceITCase.TOKEN, ConanSliceITCase.SRV_USERNAME),
                    new ItemTokenizer(Vertx.vertx()),
                    "test"
            )),
            ConanSliceITCase.CONAN_PORT
        );
        final int port = this.server.start();
        Testcontainers.exposeHostPorts(port);
        this.cntn = new GenericContainer<>(ConanSliceITCase.base)
            .withCommand("tail", "-f", "/dev/null")
            .withReuse(true)
            .withAccessToHost(true);
        this.cntn.start();
        this.cntn.execInContainer("bash", "-c", "pwd;ls -lah;env>>/tmp/conan_trace.log");
        this.cntn.execInContainer(
            "conan", "user", "-r", "conan-test", ConanSliceITCase.SRV_USERNAME, "-p", ConanSliceITCase.SRV_PASSWORD
        );
        this.cntn.execInContainer("bash", "-c", "echo 'STARTED'>>/tmp/conan_trace.log");
    }

    /**
     * Prepares base docker image instance for tests.
     *
     * @return ImageFromDockerfile of testcontainers.
     */
    @SuppressWarnings("PMD.LineLengthCheck")
    private static ImageFromDockerfile getBaseImage() {
        return new ImageFromDockerfile().withDockerfileFromBuilder(
            builder -> builder
                .from("ubuntu:22.04")
                .env("CONAN_TRACE_FILE", "/tmp/conan_trace.log")
                .env("DEBIAN_FRONTEND", "noninteractive")
                .env("CONAN_VERBOSE_TRACEBACK", "1")
                .env("CONAN_NON_INTERACTIVE", "1")
                .env("CONAN_LOGIN_USERNAME", ConanSliceITCase.SRV_USERNAME)
                .env("CONAN_PASSWORD", ConanSliceITCase.SRV_PASSWORD)
                .env("no_proxy", "host.docker.internal,host.testcontainers.internal,localhost,127.0.0.1")
                .workDir("/home")
                .run("apt clean -y && apt update -y -o APT::Update::Error-Mode=any")
                .run("apt install --no-install-recommends -y python3-pip curl g++ git make cmake")
                .run("pip3 install -U pip setuptools")
                .run("pip3 install -U conan==1.60.2")
                .run("conan profile new --detect default")
                .run("conan profile update settings.compiler.libcxx=libstdc++11 default")
                .run("conan remote add conancenter https://center.conan.io False --force")
                .run("conan remote add conan-center https://conan.bintray.com False --force")
                .run("conan remote add conan-test http://host.testcontainers.internal:9300 False --force")
                .run("conan remote disable conancenter")
                .run("conan remote disable conan-center")
                .build()
        );
    }
}

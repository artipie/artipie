/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

/**
 * Conda IT case with S3 storage.
 * @since 0.23
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle LineLengthCheck (500 lines)
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class CondaS3ITCase {

    /**
     * Curl exit code when resource not retrieved and `--fail` is used, http 400+.
     */
    private static final int CURL_NOT_FOUND = 22;

    /**
     * Repository TCP port number.
     */
    private static final String PORT = "8080";

    /**
     * Test repository name.
     */
    private static final String REPO = "my-conda";

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     * @checkstyle MagicNumberCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withUser("security/users/alice.yaml", "alice")
            .withRepoConfig("conda/conda-s3.yml", "my-conda"),
        () -> new TestDeployment.ClientContainer("continuumio/miniconda3:4.10.3")
            .withWorkingDirectory("/w")
            .withNetworkAliases("minic")
            .withExposedPorts(9000)
            .waitingFor(
                new AbstractWaitStrategy() {
                    @Override
                    protected void waitUntilReady() {
                        // Don't wait for minIO port.
                    }
                }
            )
            .withClasspathResourceMapping(
                "conda/condarc", "/w/.condarc", BindMode.READ_ONLY
            )
            .withClasspathResourceMapping(
                "conda/snappy-1.1.3-0.tar.bz2", "/w/snappy-1.1.3-0.tar.bz2", BindMode.READ_ONLY
            )
            .withClasspathResourceMapping(
                "conda/noarch_glom-22.1.0.tar.bz2", "/w/noarch_glom-22.1.0.tar.bz2", BindMode.READ_ONLY
            )
            .withClasspathResourceMapping(
                "conda/linux-64_nng-1.4.0.tar.bz2", "/w/linux-64_nng-1.4.0.tar.bz2", BindMode.READ_ONLY
            )
            .withClasspathResourceMapping(
                "minio-bin-20231120.txz", "/w/minio-bin-20231120.txz", BindMode.READ_ONLY
            )
    );

    @BeforeEach
    void init() throws IOException {
        this.containers.assertExec(
            "Apt dependencies installation failed", new ContainerResultMatcher(),
            "bash", "-c", "apt update -y; apt install -y curl"
        );
        this.containers.assertExec(
            "Conda dependencies installation failed", new ContainerResultMatcher(),
            "conda install -vv -y conda-build==3.27.0 conda-verify==3.4.2 anaconda-client==1.10.0".split(" ")
        );
        this.containers.assertExec(
            "Failed to move condarc to /root", new ContainerResultMatcher(),
            "mv", "/w/.condarc", "/root/"
        );
        this.containers.assertExec(
            "Failed to set anaconda upload url",
            new ContainerResultMatcher(),
            "anaconda", "config", "--set", "url",
            String.format(
                "http://artipie:%s/%s/",
                CondaS3ITCase.PORT,
                CondaS3ITCase.REPO
            ), "-s"
        );
        this.containers.assertExec(
            "Failed to set anaconda upload flag",
            new ContainerResultMatcher(),
            "conda config --set anaconda_upload yes".split(" ")
        );
        this.containers.assertExec(
            "Minio unpack failed", new ContainerResultMatcher(),
            "tar xvf /w/minio-bin-20231120.txz -C /root".split(" ")
        );
        this.containers.assertExec(
            "Failed to start Minio", new ContainerResultMatcher(),
            "bash", "-c", "nohup /root/bin/minio server /var/minio &"
        );
        this.containers.assertExec(
            "Failed to wait for Minio", new ContainerResultMatcher(),
            "sleep", "2"
        );
        this.containers.assertExec(
            "Failed to configure Minio alias", new ContainerResultMatcher(),
            "/root/bin/mc alias set srv1 http://localhost:9000 minioadmin minioadmin".split(" ")
        );
        this.containers.assertExec(
            "Failed to configure Minio bucket", new ContainerResultMatcher(),
            "/root/bin/mc mb srv1/buck1 --region s3test".split(" ")
        );
        this.containers.assertExec(
            "Failed to configure Minio access", new ContainerResultMatcher(),
            "/root/bin/mc anonymous set public srv1/buck1".split(" ")
        );
        this.containers.assertExec(
            "Login was not successful",
            new ContainerResultMatcher(),
            "anaconda login --username alice --password 123".split(" ")
        );
    }

    @ParameterizedTest
    @CsvSource({
        "noarch_glom-22.1.0.tar.bz2,glom/22.1.0/noarch,noarch",
        "snappy-1.1.3-0.tar.bz2,snappy/1.1.3/linux-64,linux-64"
    })
    void canSingleUploadToArtipie(final String pkgname, final String pkgpath, final String pkgarch)
        throws IOException {
        this.containers.assertExec(
            "repodata.json must be absent in S3 before test",
            new ContainerResultMatcher(new IsEqual<>(CondaS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minic:9000/buck1/my-conda/%s/repodata.json".formatted(pkgarch).split(" ")
        );
        this.containers.assertExec(
            "%s must be absent in S3 before test".formatted(pkgname),
            new ContainerResultMatcher(new IsEqual<>(CondaS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minic:9000/buck1/my-conda/%s/%s".formatted(pkgarch, pkgname).split(" ")
        );
        this.containers.assertExec(
            "Package was not uploaded successfully",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                Matchers.allOf(
                    new StringContains(
                        String.format(
                            "Using Anaconda API: http://artipie:%s/%s/",
                            CondaS3ITCase.PORT,
                            CondaS3ITCase.REPO
                        )
                    ),
                    new StringContains("Uploading file \"alice/%s/%s\"".formatted(pkgpath, pkgname)),
                    new StringContains("Upload complete")
                )
            ),
            "timeout 30s anaconda --show-traceback --verbose upload /w/%s".formatted(pkgname).split(" ")
        );
        this.containers.assertExec(
            "repodata.json must be absent in file storage since file storage must be unused",
            new ContainerResultMatcher(new IsEqual<>(2)),
            "ls", "/var/artipie/data/my-conda/%s/repodata.json".formatted(pkgarch)
        );
        this.containers.assertExec(
            "repodata.json must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minic:9000/buck1/my-conda/%s/repodata.json".formatted(pkgarch).split(" ")
        );
        this.containers.assertExec(
            "%s/%s must exist in S3 storage after test".formatted(pkgarch, pkgname),
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minic:9000/buck1/my-conda/%s/%s".formatted(pkgarch, pkgname).split(" ")
        );
    }

    @Test
    void canMultiUploadDifferentArchTest() throws IOException, InterruptedException {
        this.containers.assertExec(
            "linux-64/snappy-1.1.3-0.tar.bz2 must be absent in S3 before test",
            new ContainerResultMatcher(new IsEqual<>(CondaS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minic:9000/buck1/my-conda/linux-64/snappy-1.1.3-0.tar.bz2".split(" ")
        );
        this.containers.assertExec(
            "noarch_glom-22.1.0.tar.bz2 must be absent in S3 before test",
            new ContainerResultMatcher(new IsEqual<>(CondaS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minic:9000/buck1/my-conda/noarch/noarch_glom-22.1.0.tar.bz2".split(" ")
        );
        this.containers.assertExec(
            "noarch/repodata.json must be absent in S3 before test",
            new ContainerResultMatcher(new IsEqual<>(CondaS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minic:9000/buck1/my-conda/noarch/repodata.json".split(" ")
        );
        this.containers.assertExec(
            "linux-64/repodata.json must be absent in S3 before test",
            new ContainerResultMatcher(new IsEqual<>(CondaS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minic:9000/buck1/my-conda/linux-64/repodata.json".split(" ")
        );
        this.containers.assertExec(
            "Package was not uploaded successfully",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                Matchers.allOf(
                    new StringContains(
                        String.format(
                            "Using Anaconda API: http://artipie:%s/%s/",
                            CondaS3ITCase.PORT,
                            CondaS3ITCase.REPO
                        )
                    ),
                    // @checkstyle LineLengthCheck (1 line)
                    new StringContains("Uploading file \"alice/snappy/1.1.3/linux-64/snappy-1.1.3-0.tar.bz2\""),
                    new StringContains("Upload complete")
                )
            ),
            "timeout 30s anaconda --show-traceback --verbose upload /w/snappy-1.1.3-0.tar.bz2".split(" ")
        );
        this.containers.assertExec(
            "Package was not uploaded successfully",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                Matchers.allOf(
                    new StringContains(
                        String.format(
                            "Using Anaconda API: http://artipie:%s/%s/",
                            CondaS3ITCase.PORT,
                            CondaS3ITCase.REPO
                        )
                    ),
                    // @checkstyle LineLengthCheck (1 line)
                    new StringContains("Uploading file \"alice/glom/22.1.0/noarch/noarch_glom-22.1.0.tar.bz2\""),
                    new StringContains("Upload complete")
                )
            ),
            "timeout 30s anaconda --show-traceback --verbose upload /w/noarch_glom-22.1.0.tar.bz2".split(" ")
        );
        this.containers.assertExec(
            "linux-64/snappy-1.1.3-0.tar.bz2 must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minic:9000/buck1/my-conda/linux-64/snappy-1.1.3-0.tar.bz2".split(" ")
        );
        this.containers.assertExec(
            "oarch_glom-22.1.0.tar.bz2 must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minic:9000/buck1/my-conda/noarch/noarch_glom-22.1.0.tar.bz2".split(" ")
        );
        this.containers.assertExec(
            "noarch/repodata.json must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minic:9000/buck1/my-conda/noarch/repodata.json".split(" ")
        );
        this.containers.assertExec(
            "linux-64/repodata.json must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minic:9000/buck1/my-conda/linux-64/repodata.json".split(" ")
        );
    }

    @Test
    void canMultiUploadSameArchTest() throws IOException, InterruptedException {
        this.containers.assertExec(
            "linux-64/snappy-1.1.3-0.tar.bz2 must be absent in S3 before test",
            new ContainerResultMatcher(new IsEqual<>(CondaS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minic:9000/buck1/my-conda/linux-64/snappy-1.1.3-0.tar.bz2".split(" ")
        );
        this.containers.assertExec(
            "linux-64/linux-64_nng-1.4.0.tar.bz2 must be absent in S3 before test",
            new ContainerResultMatcher(new IsEqual<>(CondaS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minic:9000/buck1/my-conda/linux-64/linux-64_nng-1.4.0.tar.bz2".split(" ")
        );
        this.containers.assertExec(
            "repodata.json must be absent in S3 before test",
            new ContainerResultMatcher(new IsEqual<>(CondaS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minic:9000/buck1/my-conda/linux-64/repodata.json".split(" ")
        );
        this.containers.assertExec(
            "Package was not uploaded successfully",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                Matchers.allOf(
                    new StringContains(
                        String.format(
                            "Using Anaconda API: http://artipie:%s/%s/",
                            CondaS3ITCase.PORT,
                            CondaS3ITCase.REPO
                        )
                    ),
                    new StringContains("Uploading file \"alice/nng/1.4.0/linux-64/linux-64_nng-1.4.0.tar.bz2\""),
                    new StringContains("Upload complete")
                )
            ),
            "timeout 30s anaconda --show-traceback --verbose upload /w/linux-64_nng-1.4.0.tar.bz2".split(" ")
        );
        this.containers.assertExec(
            "Package was not uploaded successfully",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                Matchers.allOf(
                    new StringContains(
                        String.format(
                            "Using Anaconda API: http://artipie:%s/%s/",
                            CondaS3ITCase.PORT,
                            CondaS3ITCase.REPO
                        )
                    ),
                    new StringContains("Uploading file \"alice/snappy/1.1.3/linux-64/snappy-1.1.3-0.tar.bz2\""),
                    new StringContains("Upload complete")
                )
            ),
            "timeout 30s anaconda --show-traceback --verbose upload /w/snappy-1.1.3-0.tar.bz2".split(" ")
        );
        this.containers.assertExec(
            "linux-64/snappy-1.1.3-0.tar.bz2 must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minic:9000/buck1/my-conda/linux-64/snappy-1.1.3-0.tar.bz2".split(" ")
        );
        this.containers.assertExec(
            "linux-64/linux-64_nng-1.4.0.tar.bz2 must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minic:9000/buck1/my-conda/linux-64/linux-64_nng-1.4.0.tar.bz2".split(" ")
        );
        this.containers.assertExec(
            "linux-64/repodata.json must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minic:9000/buck1/my-conda/linux-64/repodata.json".split(" ")
        );
    }
}

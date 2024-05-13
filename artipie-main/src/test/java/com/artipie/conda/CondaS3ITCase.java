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
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

/**
 * Conda IT case with S3 storage.
 * @since 0.23
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
@Execution(ExecutionMode.CONCURRENT)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class CondaS3ITCase {

    /**
     * Curl exit code when resource not retrieved and `--fail` is used, http 400+.
     */
    private static final int CURL_NOT_FOUND = 22;

    /**
     * Repository TCP port number.
     */
    private static final int PORT = 8080;

    /**
     * MinIO S3 storage server port.
     */
    private static final int S3_PORT = 9000;

    /**
     * Test repository name.
     */
    private static final String REPO = "my-conda";

    /**
     * Test deployments.
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withUser("security/users/alice.yaml", "alice")
            .withRepoConfig("conda/conda-s3.yml", "my-conda"),
        () -> new TestDeployment.ClientContainer("artipie/conda-tests:1.0")
            .withNetworkAliases("minic")
            .withExposedPorts(CondaS3ITCase.S3_PORT)
            .waitingFor(
                new AbstractWaitStrategy() {
                    @Override
                    protected void waitUntilReady() {
                        // Don't wait for minIO port.
                    }
                }
            )
    );

    @BeforeEach
    void init() throws IOException {
        this.containers.assertExec(
            "Failed to start Minio", new ContainerResultMatcher(),
            "bash", "-c", "nohup /root/bin/minio server /var/minio 2>&1|tee /tmp/minio.log &"
        );
        this.containers.assertExec(
            "Failed to wait for Minio", new ContainerResultMatcher(),
            "timeout", "30",  "sh", "-c", "until nc -z localhost 9000; do sleep 0.1; done"
        );
        this.containers.assertExec(
            "Login was not successful",
            new ContainerResultMatcher(),
            "anaconda login --username alice --password 123".split(" ")
        );
        this.containers.assertExec(
            "Waiting for login",
            new ContainerResultMatcher(),
            "sleep 3".split(" ")
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
                            "Using Anaconda API: http://artipie:%d/%s/",
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
                            "Using Anaconda API: http://artipie:%d/%s/",
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
            "Package was not uploaded successfully",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                Matchers.allOf(
                    new StringContains(
                        String.format(
                            "Using Anaconda API: http://artipie:%d/%s/",
                            CondaS3ITCase.PORT,
                            CondaS3ITCase.REPO
                        )
                    ),
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
                            "Using Anaconda API: http://artipie:%d/%s/",
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
                            "Using Anaconda API: http://artipie:%d/%s/",
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

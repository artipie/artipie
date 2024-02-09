/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm;

import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import org.cactoos.list.ListOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import java.io.IOException;

/**
 * IT case for RPM repository.
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@DisabledOnOs(OS.WINDOWS)
public final class RpmS3ITCase {

    /**
     * Curl exit code when resource not retrieved and `--fail` is used, http 400+.
     */
    private static final int CURL_NOT_FOUND = 22;

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     * @checkstyle MagicNumberCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("rpm/my-rpm-s3.yml", "my-rpm")
            .withExposedPorts(8080),
        () -> new TestDeployment.ClientContainer(
            new ImageFromDockerfile(
                "local/artipie-main/rpm_s3_itcase", false
            ).withDockerfileFromBuilder(
                builder -> builder
                    .from("fedora:35")
                    .run("dnf -y install curl xz netcat")
                    .copy("minio-bin-20231120.txz", "/w/minio-bin-20231120.txz")
                    .run("tar xf /w/minio-bin-20231120.txz -C /root")
                    .run(
                        String.join(
                            ";",
                            "sh -c '/root/bin/minio server /var/minio > /tmp/minio.log 2>&1 &'",
                            "timeout 30 sh -c 'until nc -z localhost 9000; do sleep 0.1; done'",
                            "/root/bin/mc alias set srv1 http://localhost:9000 minioadmin minioadmin 2>&1 |tee /tmp/mc.log",
                            "/root/bin/mc mb srv1/buck1 --region s3test 2>&1|tee -a /tmp/mc.log",
                            "/root/bin/mc anonymous set public srv1/buck1 2>&1|tee -a /tmp/mc.log"
                        )
                    )
                    .run("rm -fv /w/minio-bin-20231120.txz /tmp/*.log")
            ).withFileFromClasspath("minio-bin-20231120.txz", "minio-bin-20231120.txz")
        )
        .withWorkingDirectory("/w")
        .withNetworkAliases("minioc")
        .withExposedPorts(9000)
        .withClasspathResourceMapping(
            "rpm/time-1.7-45.el7.x86_64.rpm", "/w/time-1.7-45.el7.x86_64.rpm", BindMode.READ_ONLY
        )
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
    void setUp() throws IOException {
        this.containers.assertExec(
            "Failed to start Minio", new ContainerResultMatcher(),
            "bash", "-c", "nohup /root/bin/minio server /var/minio 2>&1|tee /tmp/minio.log &"
        );
        this.containers.assertExec(
            "Failed to wait for Minio", new ContainerResultMatcher(),
            "timeout", "30",  "sh", "-c", "until nc -z localhost 9000; do sleep 0.1; done"
        );
    }

    @ParameterizedTest
    @CsvSource({
        "8080,my-rpm,9000",
    })
    void uploadsAndInstallsThePackage(final String port, final String repo, final String s3port) throws Exception {
        this.containers.assertExec(
            "rpm must be absent in S3 before test",
            new ContainerResultMatcher(new IsEqual<>(RpmS3ITCase.CURL_NOT_FOUND)),
            "curl -f -kv http://minioc:%s/buck1/%s/time-1.7-45.el7.x86_64.rpm".formatted(s3port, repo).split(" ")
        );
        this.containers.putBinaryToClient(
            String.join(
                "\n", "[example]",
                "name=Example Repository",
                String.format("baseurl=http://artipie:%s/%s", port, repo),
                "enabled=1",
                "gpgcheck=0"
            ).getBytes(),
            "/etc/yum.repos.d/example.repo"
        );
        this.containers.assertExec(
            "Failed to upload rpm package",
            new ContainerResultMatcher(),
            "timeout 30s curl http://artipie:%s/%s/time-1.7-45.el7.x86_64.rpm --upload-file /w/time-1.7-45.el7.x86_64.rpm"
                .formatted(port, repo).split(" ")
        );
        // @checkstyle MagicNumberCheck (1 line)
        Thread.sleep(2000);
        this.containers.assertExec(
            "Failed to install time package",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContainsInOrder(new ListOf<>("time-1.7-45.el7.x86_64", "Complete!"))
            ),
            "dnf", "-y", "repository-packages", "example", "install"
        );
        this.containers.assertExec(
            "rpm must exist in S3 storage after test",
            new ContainerResultMatcher(new IsEqual<>(0)),
            "curl -f -kv http://minioc:%s/buck1/%s/time-1.7-45.el7.x86_64.rpm".formatted(s3port, repo).split(" ")
        );
    }
}

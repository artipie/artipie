/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conan;

import com.artipie.asto.test.TestResource;
import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.io.IOException;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * Integration tests for Conan repository.
 * @since 0.23
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class ConanITCase {
    /**
     * Path prefix to conan repository test data in java resources.
     */
    private static final String SRV_RES_PREFIX = "conan/conan_server/data";

    /**
     * Path prefix for conan repository test data in artipie container repo.
     */
    private static final String SRV_REPO_PREFIX = "/var/artipie/data/my-conan";

    /**
     * Conan server zlib package files list for integration tests.
     */
    private static final String[] CONAN_TEST_PKG = {
        "zlib/1.2.13/_/_/0/package/dfbe50feef7f3c6223a476cd5aeadb687084a646/0/conaninfo.txt",
        "zlib/1.2.13/_/_/0/package/dfbe50feef7f3c6223a476cd5aeadb687084a646/0/conan_package.tgz",
        "zlib/1.2.13/_/_/0/package/dfbe50feef7f3c6223a476cd5aeadb687084a646/0/conanmanifest.txt",
        "zlib/1.2.13/_/_/0/package/dfbe50feef7f3c6223a476cd5aeadb687084a646/revisions.txt",
        "zlib/1.2.13/_/_/0/export/conan_export.tgz",
        "zlib/1.2.13/_/_/0/export/conanfile.py",
        "zlib/1.2.13/_/_/0/export/conanmanifest.txt",
        "zlib/1.2.13/_/_/0/export/conan_sources.tgz",
        "zlib/1.2.13/_/_/revisions.txt",
    };

    /**
     * Test deployments.
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withUser("security/users/alice.yaml", "alice")
            .withRepoConfig("conan/conan.yml", "my-conan")
            .withExposedPorts(9301),
        ConanITCase::prepareClientContainer
    );

    @Test
    public void incorrectPortFailTest() throws IOException {
        for (final String file : ConanITCase.CONAN_TEST_PKG) {
            this.containers.putResourceToArtipie(
                String.join("/", ConanITCase.SRV_RES_PREFIX, file),
                String.join("/", ConanITCase.SRV_REPO_PREFIX, file)
            );
        }
        this.containers.assertExec(
            "Conan remote add failed", new ContainerResultMatcher(),
            "conan remote add -f conan-test http://artipie:9300 False".split(" ")
        );
        this.containers.assertExec(
            "Conan remote add failed", new ContainerResultMatcher(
                new IsNot<>(new IsEqual<>(ContainerResultMatcher.SUCCESS))
            ),
            "conan install zlib/1.2.13@ -r conan-test -b -pr:b=default".split(" ")
        );
    }

    @Test
    public void incorrectPkgFailTest() throws IOException {
        for (final String file : ConanITCase.CONAN_TEST_PKG) {
            this.containers.putResourceToArtipie(
                String.join("/", ConanITCase.SRV_RES_PREFIX, file),
                String.join("/", ConanITCase.SRV_REPO_PREFIX, file)
            );
        }
        this.containers.assertExec(
            "Conan remote add failed", new ContainerResultMatcher(
                new IsNot<>(new IsEqual<>(ContainerResultMatcher.SUCCESS))
            ),
            "conan install zlib/1.2.11@ -r conan-test -b -pr:b=default".split(" ")
        );
    }

    @Test
    public void installFromArtipie() throws IOException {
        for (final String file : ConanITCase.CONAN_TEST_PKG) {
            this.containers.putResourceToArtipie(
                String.join("/", ConanITCase.SRV_RES_PREFIX, file),
                String.join("/", ConanITCase.SRV_REPO_PREFIX, file)
            );
        }
        this.containers.assertExec(
            "Conan install failed", new ContainerResultMatcher(),
            "conan install zlib/1.2.13@ -r conan-test".split(" ")
        );
    }

    @Test
    public void uploadToArtipie() throws IOException {
        this.containers.assertExec(
            "Conan install failed", new ContainerResultMatcher(),
            "conan install zlib/1.2.13@ -r conancenter".split(" ")
        );
        this.containers.assertExec(
            "Conan upload failed", new ContainerResultMatcher(),
            "conan upload zlib/1.2.13@ -r conan-test --all".split(" ")
        );
    }

    @Test
    public void uploadFailtest() throws IOException {
        this.containers.assertExec(
            "Conan upload failed", new ContainerResultMatcher(
                new IsNot<>(new IsEqual<>(ContainerResultMatcher.SUCCESS))
            ),
            "conan upload zlib/1.2.13@ -r conan-test --all".split(" ")
        );
    }

    @Test
    void testPackageReupload() throws IOException, InterruptedException {
        this.containers.assertExec(
            "Conan install (conancenter) failed", new ContainerResultMatcher(),
            "conan install zlib/1.2.13@ -r conancenter".split(" ")
        );
        this.containers.assertExec(
            "Conan upload failed", new ContainerResultMatcher(),
            "conan upload zlib/1.2.13@ -r conan-test --all".split(" ")
        );
        this.containers.assertExec(
            "rm cache failed", new ContainerResultMatcher(),
            "rm -rf /root/.conan/data".split(" ")
        );
        this.containers.assertExec(
            "Conan install (conan-test) failed", new ContainerResultMatcher(),
            "conan install zlib/1.2.13@ -r conan-test".split(" ")
        );
    }

    /**
     * Prepares base docker image instance for tests.
     *
     * @return ImageFromDockerfile of testcontainers.
     */
    @SuppressWarnings("PMD.LineLengthCheck")
    private static TestDeployment.ClientContainer prepareClientContainer() {
        final ImageFromDockerfile image = new ImageFromDockerfile("local/artipie-main/conan_itcase", false).withDockerfileFromBuilder(
            builder -> builder
                .from("ubuntu:22.04")
                .env("CONAN_TRACE_FILE", "/tmp/conan_trace.log")
                .env("DEBIAN_FRONTEND", "noninteractive")
                .env("CONAN_VERBOSE_TRACEBACK", "1")
                .env("CONAN_NON_INTERACTIVE", "1")
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
                .run("conan remote add conan-test http://artipie:9301 False --force")
        );
        return new TestDeployment.ClientContainer(image)
            .withCommand("tail", "-f", "/dev/null")
            .withReuse(true);
    }
}

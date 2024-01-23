/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget;

import com.artipie.asto.test.TestResource;
import com.artipie.test.ContainerResultMatcher;
import com.artipie.test.TestDeployment;
import java.util.Arrays;
import java.util.UUID;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Integration tests for Nuget repository.
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class NugetITCase {

    /**
     * Test deployments.
             */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("nuget/nuget.yml", "my-nuget")
            .withRepoConfig("nuget/nuget-port.yml", "my-nuget-port")
            .withExposedPorts(8081),
        () -> new TestDeployment.ClientContainer("mcr.microsoft.com/dotnet/sdk:5.0")
            .withWorkingDirectory("/w")
    );

    @BeforeEach
    void init() {
        this.containers.putBinaryToClient(
            String.join(
                "",
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n",
                "<configuration>",
                "<disabledPackageSources>",
                "<add key=\"nuget.org\" value=\"true\" />",
                "</disabledPackageSources>",
                "</configuration>"
            ).getBytes(), "/w/NuGet.Config"
        );
    }

    @ParameterizedTest
    @CsvSource({
        "8080,my-nuget",
        "8081,my-nuget-port"
    })
    void shouldPushAndInstallPackage(final String port, final String repo) throws Exception {
        final String pckgname = UUID.randomUUID().toString();
        this.containers.putBinaryToClient(
            new TestResource("nuget/newtonsoft.json/12.0.3/newtonsoft.json.12.0.3.nupkg").asBytes(),
            String.format("/w/%s", pckgname)
        );
        this.containers.assertExec(
            "Package was not pushed",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContains("Your package was pushed.")
            ),
            "dotnet", "nuget", "push", pckgname, "-s",
            String.format("http://artipie:%s/%s/index.json", port, repo)
        );
        this.containers.assertExec(
            "New project was not created",
            new ContainerResultMatcher(),
            "dotnet", "new", "console", "-n", "TestProj"
        );
        this.containers.assertExec(
            "Package was not added",
            new ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContainsInOrder(
                    Arrays.asList(
                        "PackageReference for package 'newtonsoft.json' version '12.0.3' added to file '/w/TestProj/TestProj.csproj'",
                        "Restored /w/TestProj/TestProj.csproj"
                    )
                )
            ),
            "dotnet", "add", "TestProj", "package", "newtonsoft.json",
            "--version", "12.0.3", "-s",
            String.format("http://artipie:%s/%s/index.json", port, repo)
        );
    }

}

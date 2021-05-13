/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.nuget;

import com.artipie.asto.test.TestResource;
import com.artipie.maven.MavenITCase;
import com.artipie.test.TestDeployment;
import java.util.Arrays;
import java.util.UUID;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Integration tests for Nuget repository.
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class NugetITCase {

    /**
     * Test deployments.
     * @checkstyle VisibilityModifierCheck (10 lines)
     */
    @RegisterExtension
    final TestDeployment containers = new TestDeployment(
        () -> TestDeployment.ArtipieContainer.defaultDefinition()
            .withRepoConfig("nuget/nuget.yml", "my-nuget"),
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

    @Test
    void shouldPushPackage() throws Exception {
        final String pckgname = UUID.randomUUID().toString();
        this.containers.putBinaryToClient(
            new TestResource("nuget/newtonsoft.json/12.0.3/newtonsoft.json.12.0.3.nupkg").asBytes(),
            String.format("/w/%s", pckgname)
        );
        this.containers.assertExec(
            "Package was not pushed",
            new MavenITCase.ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContains("Your package was pushed.")
            ),
            "dotnet", "nuget", "push", pckgname, "-s", "http://artipie:8080/my-nuget/index.json"
        );
        this.containers.assertExec(
            "New project was not created",
            new MavenITCase.ContainerResultMatcher(),
            "dotnet", "new", "console", "-n", "TestProj"
        );
        this.containers.assertExec(
            "Package was not added",
            new MavenITCase.ContainerResultMatcher(
                new IsEqual<>(0),
                new StringContainsInOrder(
                    Arrays.asList(
                        // @checkstyle LineLengthCheck (1 line)
                        "PackageReference for package 'newtonsoft.json' version '12.0.3' added to file '/w/TestProj/TestProj.csproj'",
                        "Restored /w/TestProj/TestProj.csproj"
                    )
                )
            ),
            "dotnet", "add", "TestProj", "package", "newtonsoft.json",
            "--version", "12.0.3", "-s", "http://artipie:8080/my-nuget/index.json"
        );
    }

}

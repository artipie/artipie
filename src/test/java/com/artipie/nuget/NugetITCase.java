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

import com.artipie.ArtipieServer;
import com.artipie.RepoConfigYaml;
import com.artipie.asto.Key;
import com.artipie.asto.fs.FileStorage;
import com.jcabi.log.Logger;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

/**
 * Integration tests for Nuget repository.
 * @since 0.12
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class NugetITCase {

    /**
     * URL.
     */
    private static final String URL = "http://host.testcontainers.internal:%d/my-nuget/index.json";

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    /**
     * Tested Artipie server.
     */
    private ArtipieServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Server port.
     */
    private int port;

    /**
     * For puzzle.
     * @todo #602:30min Create constructor in `ArtipieServer` to pass server port.
     *  Now config for server are generated earlier than server starts. So, we
     *  need to overwrite file with config after server starting. Let's create
     *  constructor in `ArtipieServer` to pass free port.
     */
    @BeforeEach
    void init() throws Exception {
        final String name = "my-nuget";
        try (ServerSocket socket = new ServerSocket(0)) {
            this.port = socket.getLocalPort();
            this.server = new ArtipieServer(this.tmp, name, this.config());
        }
        this.port = this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        new FileStorage(this.tmp)
            .save(new Key.From(String.format("repos/%s.yaml", name)), this.config().toContent())
            .join();
        this.createNugetConfig();
        this.cntn = new GenericContainer<>("mcr.microsoft.com/dotnet/sdk:5.0")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.cntn.stop();
    }

    @Test
    @Timeout(10)
    void shouldPushPackage() throws Exception {
        MatcherAssert.assertThat(
            this.pushPackage(),
            new StringContains("Your package was pushed.")
        );
    }

    @Test
    @Timeout(30)
    void shouldInstallPushedPackage() throws Exception {
        this.pushPackage();
        this.exec("dotnet", "new", "console", "-n", "TestProj");
        MatcherAssert.assertThat(
            this.exec(
                "dotnet", "add", "TestProj", "package", "newtonsoft.json",
                "--version", "12.0.3", "-s", String.format(NugetITCase.URL, this.port)
            ),
            new StringContainsInOrder(
                Arrays.asList(
                    // @checkstyle LineLengthCheck (1 line)
                    "PackageReference for package 'newtonsoft.json' version '12.0.3' added to file '/home/TestProj/TestProj.csproj'",
                    "Restored /home/TestProj/TestProj.csproj"
                )
            )
        );
    }

    private void createNugetConfig() throws Exception {
        Files.write(
            this.tmp.resolve("NuGet.Config"),
            String.join(
                "",
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n",
                "<configuration>",
                "<disabledPackageSources>",
                "<add key=\"nuget.org\" value=\"true\" />",
                "</disabledPackageSources>",
                "</configuration>"
            ).getBytes()
        );
    }

    private RepoConfigYaml config() {
        return new RepoConfigYaml("nuget")
            .withFileStorage(this.tmp.resolve("repos"))
            .withUrl(String.format("http://host.testcontainers.internal:%d/my-nuget", this.port));
    }

    private String exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s", String.join(" ", command));
        return this.cntn.execInContainer(command).getStdout();
    }

    private String pushPackage() throws Exception {
        final String pckgname = UUID.randomUUID().toString();
        Files.write(
            this.tmp.resolve(pckgname),
            new NewtonJsonResource("newtonsoft.json.12.0.3.nupkg").bytes()
        );
        return this.exec(
            "dotnet", "nuget", "push", pckgname,
            "-s", String.format(NugetITCase.URL, this.port)
        );
    }
}

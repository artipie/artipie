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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
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
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
final class NugetITCase {

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
     * Artipie server port.
     */
    private int port;

    @BeforeEach
    void init() throws Exception {
        final String name = "my-nuget";
        this.server = new ArtipieServer(this.tmp, name, this.config());
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

    // @checkstyle MagicNumberCheck (2 lines)
    @Test
    @Timeout(10)
    void shouldPushPackage() throws Exception {
        MatcherAssert.assertThat(
            this.pushPackage(),
            new StringContains("Your package was pushed.")
        );
    }

    private void createNugetConfig() throws Exception {
        final String url = String.format(
            "http://host.testcontainers.internal:%d/my-nuget", this.port
        );
        final String source = "artipie-nuget-test";
        Files.write(
            this.tmp.resolve("NuGet.Config"),
            String.join(
                "",
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n",
                "<configuration>",
                "<packageSources>",
                String.format("<add key=\"%s\" value=\"%s\"/>", source, url),
                "</packageSources>",
                "</configuration>"
            ).getBytes()
        );
    }

    private RepoConfigYaml config() {
        return new RepoConfigYaml("nuget")
            .withFileStorage(this.tmp.resolve("repos"))
            .withUrl(
                String.format("http://host.testcontainers.internal:%d/my-nuget", this.port)
            );
    }

    private String exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s", String.join(" ", command));
        return this.cntn.execInContainer(command).getStdout();
    }

    private String pushPackage() throws Exception {
        final String uri = "http://host.testcontainers.internal:%d/my-nuget/index.json";
        final String pckg = UUID.randomUUID().toString();
        Files.write(
            this.tmp.resolve(pckg),
            new NewtonJsonResource("newtonsoft.json.12.0.3.nupkg").bytes()
        );
        return this.exec(
            "dotnet", "nuget", "push", pckg, "--api-key", "this.source",
            "-s", String.format(uri, this.port)
        );
    }
}

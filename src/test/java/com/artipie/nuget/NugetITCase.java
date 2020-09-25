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

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.ArtipieServer;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.http.auth.Permissions;
import com.jcabi.log.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;

/**
 * Integration tests for Nuget repository.
 * @since 0.11
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class NugetITCase {

    /**
     * Username.
     */
    private static final String USER = "Aladdin";

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    /**
     * Subdirectory in temporary directory.
     */
    private Path subdir;

    /**
     * Tested Artipie server.
     */
    private ArtipieServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Storage.
     */
    private Storage storage;

    /**
     * Artipie server port.
     */
    private int port;

    /**
     * Packages source name in config.
     */
    private String source;

    /**
     * NuGet config file path.
     */
    private Path config;

    @BeforeEach
    void init() throws Exception {
        final String name = "my-nuget";
        this.subdir = Files.createDirectory(Path.of(this.tmp.toString(), "subdir"));
        this.storage = new FileStorage(this.subdir);
        this.server = new ArtipieServer(this.subdir, name, this.configs());
        this.port = this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        Files.write(
            this.subdir.resolve(String.format("repos/%s.yaml", name)),
            this.configs().getBytes()
        );
        this.settings();
        this.cntn = new GenericContainer<>("centos:centos8")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.subdir.toString(), "/home");
        this.cntn.start();
        System.out.println();
        this.exec("yum", "-y", "install", "sudo");
        this.exec("sudo", "rpm", "-Uvh", "https://packages.microsoft.com/config/rhel/7/packages-microsoft-prod.rpm");
        this.exec("sudo", "yum", "makecache");
        this.exec("sudo", "yum", "install", "-y", "dotnet-sdk-2.2");
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.cntn.stop();
    }

    @Test
    void shouldPushPackage() throws Exception {
        final String file = UUID.randomUUID().toString();
        Files.write(
            this.subdir.resolve(file),
            new NewtonJsonResource("newtonsoft.json.12.0.3.nupkg").bytes()
        );
        this.exec("cd", this.subdir.toString());
        final String res = this.runNuGet("push", file);
        System.out.println(res);
//        MatcherAssert.assertThat(
//            res,
//            new StringContains("Your package was pushed.")
//        );
    }

    private void settings() throws Exception {
        final String pswd = "OpenSesame";
        final String base = String.format(
            "http://host.testcontainers.internal:%d/my-nuget", this.port
        );
        this.source = "artipie-nuget-test";
        this.config = this.subdir.resolve("NuGet.Config");
        Files.write(
            this.config,
            this.configXml(
                String.format("%s/index.json", base), NugetITCase.USER, pswd
            )
        );
    }

    private String configs() {
        return Yaml.createYamlMappingBuilder().add(
            "repo",
            Yaml.createYamlMappingBuilder()
                .add("type", "nuget")
                .add("url", String.format(
                    "http://host.testcontainers.internal:%d/my-nuget", this.port
                ))
                .add(
                    "storage",
                    Yaml.createYamlMappingBuilder()
                        .add("type", "fs")
                        .add("path", this.subdir.resolve("repos").toString())
                        .build()
                )
                .build()
        ).build().toString();
    }

    private byte[] configXml(final String url, final String user, final String pwd) {
        return String.join(
            "",
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n",
            "<configuration>",
            "<packageSources>",
            String.format("<add key=\"%s\" value=\"%s\"/>", this.source, url),
            "</packageSources>",
            "<packageSourceCredentials>",
            String.format("<%s>", this.source),
            String.format("<add key=\"Username\" value=\"%s\"/>", user),
            String.format("<add key=\"ClearTextPassword\" value=\"%s\"/>", pwd),
            String.format("</%s>", this.source),
            "</packageSourceCredentials>",
            "</configuration>"
        ).getBytes();
    }

    private String exec(final String... command) throws Exception {
        Logger.debug(this, "Command:\n%s", String.join(" ", command));
        return this.cntn.execInContainer(command).getStdout();
    }

    private String runNuGet(final String... args) throws Exception {
        final String res = this.exec(
            "dotnet", "nuget", args[0], args[1], "--api-key", this.source,
            "-s", String.format(
                "http://host.testcontainers.internal:%d/my-nuget/index.json", this.port
            )
        );
        return res;
    }

    private Permissions permissions() {
        final Permissions permissions;
        if (isWindows()) {
            permissions = (name, action) -> NugetITCase.USER.equals(name);
        } else {
            permissions = Permissions.FREE;
        }
        return permissions;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }
}

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
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.auth.Permissions;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.nuget.http.NuGet;
import com.artipie.vertx.VertxSliceServer;
import com.google.common.collect.ImmutableList;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.cactoos.list.ListOf;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.MatchesPattern;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

/**
 * Integration tests for Nuget repository.
 * @since 0.11
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.WINDOWS})
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
//        final Path setting = this.subdir.resolve("settings.xml");
//        setting.toFile().createNewFile();
//        Files.write(setting, this.settings());
        this.settings();
//        this.cntn = new GenericContainer<>("mcr.microsoft.com/dotnet/core/runtime:3.0")
        this.cntn = new GenericContainer<>("centos:centos8")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
//            .withEnv(new MapOf<String, String>(new MapEntry<>("$PATH", "nuget.exe"), new MapEntry<>("$PATH", "nuget")))
            .withFileSystemBind(this.subdir.toString(), "/home");
        this.cntn.start();
//        this.exec("docker", "pull", "mcr.microsoft.com/dotnet/runtime:5.0");
//        System.out.println(this.exec("apt-get", "search", "maven"));
//        System.out.println(this.exec("yum", "search", "maven"));
//        System.out.println(this.exec("dnf", "search", "maven"));
//        System.out.println(this.exec("pkg", "search", "maven"));
//        System.out.println(this.exec("apt", "-y", "install", "maven"));
//        System.out.println(this.exec("mvn", "--version"));
        System.out.println();
        this.exec("yum", "-y", "install", "sudo");
        this.exec("sudo", "rpm", "-Uvh", "https://packages.microsoft.com/config/rhel/7/packages-microsoft-prod.rpm");
        this.exec("sudo", "yum", "makecache");
        System.out.println(this.exec("sudo", "yum", "install", "-y", "dotnet-sdk-2.2"));
//        System.out.println(this.exec("sudo", "apt", "install", "-y", "apt-transport-https"));
//        System.out.println(this.exec("sudo", "dnf", "config-manager", "--add-repo=https://download.docker.com/linux/centos/docker-ce.repo"));
//        System.out.println(this.exec("yum", "-y", "install", "docker"));
//        System.out.println(this.exec("docker", "pull", "mcr.microsoft.com/dotnet/core/runtime:3.1"));
//        System.out.println(this.exec("sudo", "yum", "-y", "install", "docker"));
        System.out.println();
//        System.out.println(this.exec("curl", "-o", "nuget.exe",  "https://dist.nuget.org/win-x86-commandline/latest/nuget.exe"));

//        System.out.println(this.exec("sudo", "apt-get", "install", "monocomplete"));
//        System.out.println(this.cntn.execInContainer("apt-get", "-y", "install", "nuget").getStdout());
//        System.out.println(this.exec("yum", "-y", "install", "mono-complete"));
//        this.cntn.execInContainer("wget", "https://dist.nuget.org/win-x86-commandline/latest/nuget.exe");
//        this.cntn.execInContainer("curl", "https://download-ib01.fedoraproject.org/pub/epel/7/x86_64/Packages/e/epel-release-7-12.noarch.rpm");
//        System.out.println(this.exec("rpm", "-Uvh", "epel-release-7-12.noarch.rpm"));
//        System.out.println(this.exec("yum", "install", "nuget"));
//        System.out.println(this.exec("mono-devel", "nuget.exe"));

//        System.out.println(this.cntn.execInContainer("docker", "curl", "-o", "nuget.exe", "https://dist.nuget.org/win-x86-commandline/latest/nuget.exe"));

//        System.out.println(this.cntn.execInContainer("yum", "-y", "install", "mono").getStdout());
//        System.out.println(this.cntn.execInContainer("mkdir", "-p", "/tmp/dependencies").getStdout());
//        System.out.println(this.cntn.execInContainer("cd", "/tmp/dependencies").getStdout());
//        System.out.println(this.cntn.execInContainer("wget", "https://dl.fedoraproject.org/pub/fedora/linux/development/rawhide/Everything/x86_64/os/Packages/l/libpng15-1.5.28-2.fc26.x86_64.rpm").getStdout());
//        System.out.println(this.cntn.execInContainer("yum", "install", "-y", "libpng15-1.5.28-2.fc26.x86_64.rpm").getStdout());
//        System.out.println(this.cntn.execInContainer("yum", "install", "-y", "yum-utils").getStdout());
//        System.out.println(this.cntn.execInContainer("rpm", "import", "-y", "\"http://keyserver.ubuntu.com/pks/lookup?op=get&search=0x3FA7E0328081BFF6A14DA29AA6A19B38D3D831EF\"").getStdout());
//        System.out.println(this.cntn.execInContainer("yum-config-manager", "--add-repo", "http://download.mono-project.com/repo/centos/").getStdout());
//        System.out.println(this.cntn.execInContainer("yum", "clean", "all").getStdout());
//        System.out.println(this.cntn.execInContainer("yum", "makecache").getStdout());
//        System.out.println("!!!!!!!!!!!!!");
//        System.out.println(this.cntn.execInContainer("yum", "install", "-y", "mono-complete", "nuget").getStdout());
//        System.out.println(this.cntn.execInContainer("cd").getStdout());
//        System.out.println(this.cntn.execInContainer("rm", "-rf", "/tmp/dependencies").getStdout());

//        System.out.println(this.cntn.execInContainer("mount", "--make-shared", "/").getStdout());
        //sudo systemctl show --property=MountFlags docker.service
//        System.out.println(this.cntn.execInContainer("sudo", "systemctl", "show", "--property=MountFlags", "docker.service").getStdout());
//        System.out.println(this.exec("sudo", "docker", "-v"));
//        System.out.println(this.cntn.getContainerInfo());
//        System.out.println(this.exec("nuget.exe", "install"));
//        System.out.println(this.exec("nuget.exe", "-version"));
//        System.out.println(this.cntn.execInContainer("sudo", "nuget", "help").getStdout());
//        System.out.println("something should be printed");
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
//            str,
//            new StringContains("Your package was pushed.")
//        );
    }

//    @Test
//    void shouldInstallPushedPackage() throws Exception {
//        this.pushPackage();
//        MatcherAssert.assertThat(
//            runNuGet(
//                "install",
//                "Newtonsoft.Json", "-Version", "12.0.3",
//                "-NoCache"
//            ),
//            Matchers.containsString("Successfully installed 'Newtonsoft.Json 12.0.3'")
//        );
//    }

    private void settings() throws Exception {
        final String pswd = "OpenSesame";
//        final int port = 8080;
//        final String base = String.format("http://localhost:%s", port);
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

    private String pushPackage() throws Exception {
        final String file = UUID.randomUUID().toString();
        Files.write(
            this.subdir.resolve(file),
            new NewtonJsonResource("newtonsoft.json.12.0.3.nupkg").bytes()
        );
//        this.exec(
//            "nuget.exe", "-s", "/home/settings.xml", "dependency:get",
//            String.format("-Dartifact=com.artipie:%s:%s", type, vers)
//        ).replaceAll("\n", "");
        System.out.println(this.exec(
            //nuget install Newtonsoft.Json -OutputDirectory packages
            "nuget", "install", "Newtonsoft.Json", "-OutputDirectory", "packages"
        ).replaceAll("\n", ""));
        return this.runNuGet("push", file);
    }

    private byte[] configXml(final String url, final String user, final String pwd) {
        return String.join(
            "",
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n",
            "<configuration>",
            "<packageSources>",
            String.format("<add key=\"%s\" value=\"%s\" protocolVersion=\"2048\"/>", this.source, url),
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
//        final Path stdout = this.subdir.resolve(
//            String.format("%s-stdout.txt", UUID.randomUUID().toString())
//        );
        final String res = this.exec(
            /*NugetITCase.command()*/"dotnet", "nuget", args[0], args[1], "--api-key", this.source,
            "-s", String.format(
                "http://host.testcontainers.internal:%d/my-nuget/index.json", this.port
            )
        );//.replaceAll("\n", "");
//        final int code = new ProcessBuilder()
//            .directory(this.subdir.toFile())
//            .command(
//             ImmutableList.<String>builder()
//                 .add(NugetITCase.command())
//                 .add(args)
//                 .add("-ConfigFile", this.config.toString())
//                 .add("-Source", this.source)
//                 .add("-Verbosity", "detailed")
//                 .build()
//            )
//            .redirectOutput(stdout.toFile())
//            .redirectErrorStream(true)
//            .start()
//            .waitFor();
//        final String log = new String(Files.readAllBytes(stdout));
//        Logger.debug(this, "Full stdout/stderr:\n%s", log);
//        if (code != 0) {
//            throw new IllegalStateException(String.format("Not OK exit code: %d", code));
//        }
        return res;
    }

    private static String command() {
        final String cmd;
        if (isWindows()) {
            cmd = "nuget.exe";
        } else {
            cmd = "nuget";
        }
        return cmd;
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

//    @ParameterizedTest
//    @CsvSource({"helloworld,0.1", "snapshot,1.0-SNAPSHOT"})
//    void downloadsArtifact(final String type, final String vers) throws Exception {
//        this.addFilesToStorage(
//            String.format("com/artipie/%s", type),
//            new Key.From("repos", "my-maven", "com", "artipie", type)
//        );
//        MatcherAssert.assertThat(
//            this.exec(
//                "mvn", "-s", "/home/settings.xml", "dependency:get",
//                String.format("-Dartifact=com.artipie:%s:%s", type, vers)
//            ).replaceAll("\n", ""),
//            new StringContainsInOrder(
//                new ListOf<String>(
//                    // @checkstyle LineLengthCheck (2 lines)
//                    String.format(
//                        "Downloaded from my-maven: http://host.testcontainers.internal:%d/my-maven/com/artipie/%s/%s/%s-%s.jar",
//                        this.port, type, vers, type, vers
//                    ),
//                    "BUILD SUCCESS"
//                )
//            )
//        );
//    }
//
//    @ParameterizedTest
//    @CsvSource({"helloworld,0.1,0.1", "snapshot,1.0-SNAPSHOT,1.0-[\\d-.]{17}"})
//    void deploysArtifact(final String type, final String vers, final String assembly)
//        throws Exception {
//        this.prepareDirectory(
//            String.format("%s-src", type),
//            String.format("%s-src/pom.xml", type)
//        );
//        MatcherAssert.assertThat(
//            "Build failure",
//            this.exec(
//                "mvn", "-s", "/home/settings.xml", "-f",
//                String.format("/home/%s-src/pom.xml", type),
//                "deploy"
//            ).replaceAll("\n", ""),
//            new StringContains("BUILD SUCCESS")
//        );
//        this.exec(
//            "mvn", "-s", "/home/settings.xml", "-f",
//            String.format("/home/%s-src/pom.xml", type),
//            "clean"
//        );
//        MatcherAssert.assertThat(
//            "Artifacts weren't added to storage",
//            this.storage.list(
//                new Key.From("repos", "my-maven", "com", "artipie", type)
//            ).join().stream()
//                .map(Key::string)
//                .collect(Collectors.toList())
//                .toString()
//                .replaceAll("\n", ""),
//            new AllOf<>(
//                Arrays.asList(
//                    new MatchesPattern(
//                        Pattern.compile(
//                            String.format(
//                                ".*repos/my-maven/com/artipie/%s/maven-metadata.xml.*", type
//                            )
//                        )
//                    ),
//                    new MatchesPattern(
//                        Pattern.compile(
//                            String.format(
//                                ".*repos/my-maven/com/artipie/%s/%s/%s-%s.pom.*",
//                                type, vers, type, assembly
//                            )
//                        )
//                    ),
//                    new MatchesPattern(
//                        Pattern.compile(
//                            String.format(
//                                ".*repos/my-maven/com/artipie/%s/%s/%s-%s.jar.*",
//                                type, vers, type, assembly
//                            )
//                        )
//                    )
//                )
//            )
//        );
//    }
//
//    @AfterEach
//    void release() {
//        this.server.stop();
//        this.cntn.stop();
//    }
//
//    private void prepareDirectory(final String src, final String pom) throws IOException {
//        FileUtils.copyDirectory(
//            new TestResource(src).asPath().toFile(),
//            this.subdir.resolve(src).toFile()
//        );
//        Files.write(
//            this.subdir.resolve(pom),
//            String.format(
//                Files.readString(this.subdir.resolve(pom)),
//                this.port
//            ).getBytes()
//        );
//    }
//
//    private String exec(final String... command) throws Exception {
//        Logger.debug(this, "Command:\n%s", String.join(" ", command));
//        return this.cntn.execInContainer(command).getStdout();
//    }
//
//    private List<String> settings() {
//        return new ListOf<String>(
//            "<settings>",
//            "    <profiles>",
//            "        <profile>",
//            "            <id>artipie</id>",
//            "            <repositories>",
//            "                <repository>",
//            "                    <id>my-nuget</id>",
//            String.format("<url>http://host.testcontainers.internal:%d/my-nuget/</url>", this.port),
//            "                </repository>",
//            "            </repositories>",
//            "        </profile>",
//            "    </profiles>",
//            "    <activeProfiles>",
//            "        <activeProfile>artipie</activeProfile>",
//            "    </activeProfiles>",
//            "</settings>"
//        );
//    }
//
//
//    private void addFilesToStorage(final String resource, final Key key)
//        throws InterruptedException {
//        final Storage resources = new FileStorage(
//            new TestResource(resource).asPath()
//        );
//        final BlockingStorage bsto = new BlockingStorage(resources);
//        for (final Key item : bsto.list(Key.ROOT)) {
//            new BlockingStorage(this.storage).save(
//                new Key.From(key, item),
//                bsto.value(new Key.From(item))
//            );
//        }
//    }
}

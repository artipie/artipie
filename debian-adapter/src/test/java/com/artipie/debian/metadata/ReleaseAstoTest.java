/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian.metadata;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.debian.AstoGzArchive;
import com.artipie.debian.Config;
import com.artipie.http.slice.KeyFromPath;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Test for {@link Release.Asto}.
 * @since 0.2
 */
class ReleaseAstoTest {

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createsReleaseFile(final boolean gpg) {
        new TestResource("Packages.gz")
            .saveTo(this.asto, new Key.From("dists/abc/main/binary-amd64/Packages.gz"));
        new TestResource("Packages.gz")
            .saveTo(this.asto, new Key.From("dists/abc/main/binary-intel/Packages.gz"));
        final Release release = new Release.Asto(
            this.asto,
            this.config(
                gpg, "abc",
                Yaml.createYamlMappingBuilder()
                    .add("Components", "main")
                    .add("Architectures", "amd intel")
            )
        );
        this.asto.save(release.gpgSignatureKey(), Content.EMPTY).join();
        release.create().toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Correct release file was created",
            this.asto.value(new KeyFromPath("dists/abc/Release")).join().asString(),
            Matchers.allOf(
                new StringContainsInOrder(
                    new ListOf<>(
                        "Codename: abc",
                        "Architectures: amd intel",
                        "Components: main",
                        "Date:",
                        "SHA256:"
                    )
                ),
                new StringContains(" eb8cb7a51d9fe47bde0a32a310b93c01dba531c6f8d14362552f65fcc4277af8 1351 main/binary-amd64/Packages.gz\n"),
                new StringContains(" c1cfc96b4ca50645c57e10b65fcc89fd1b2b79eb495c9fa035613af7ff97dbff 2564 main/binary-amd64/Packages\n"),
                new StringContains(" eb8cb7a51d9fe47bde0a32a310b93c01dba531c6f8d14362552f65fcc4277af8 1351 main/binary-intel/Packages.gz\n"),
                new StringContains(" c1cfc96b4ca50645c57e10b65fcc89fd1b2b79eb495c9fa035613af7ff97dbff 2564 main/binary-intel/Packages\n")
            )
        );
        MatcherAssert.assertThat(
            "Gpg file was created if necessary",
            this.asto.exists(new KeyFromPath("dists/abc/Release.gpg")).join(),
            new IsEqual<>(gpg)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createsReleaseWhenNoPackagesExist(final boolean gpg) {
        new Release.Asto(
            this.asto,
            this.config(
                gpg, "my-super-deb",
                Yaml.createYamlMappingBuilder()
                    .add("Components", "main")
                    .add("Architectures", "arm")
            )
        ).create().toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Release file was created",
            this.asto.value(new KeyFromPath("dists/my-super-deb/Release")).join().asString(),
            new StringContainsInOrder(
                new ListOf<>(
                    "Codename: my-super-deb",
                    "Architectures: arm",
                    "Components: main",
                    "Date:",
                    "SHA256:"
                )
            )
        );
        MatcherAssert.assertThat(
            "Gpg file was created if necessary",
            this.asto.exists(new KeyFromPath("dists/my-super-deb/Release.gpg")).join(),
            new IsEqual<>(gpg)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void addsNewRecord(final boolean gpg) throws IOException {
        this.asto.save(
            new Key.From("dists/my-deb/main/binary-amd64/Packages.gz"), Content.EMPTY
        ).join();
        final Key key = new Key.From("dists/my-deb/main/binary-intel/Packages.gz");
        new AstoGzArchive(this.asto).packAndSave("abc123", key);
        final ListOf<String> content = new ListOf<>(
            "Codename: my-deb",
            "Architectures: amd64 intel",
            "Components: main",
            "Date:",
            "SHA256:",
            " abc123 2 main/binaty-amd64/Packages.gz"
        );
        this.asto.save(
            new Key.From("dists/my-deb/Release"),
            new Content.From(String.join("\n", content).getBytes(StandardCharsets.UTF_8))
        ).join();
        final Release release = new Release.Asto(
            this.asto,
            this.config(gpg, "my-deb", Yaml.createYamlMappingBuilder())
        );
        this.asto.save(release.gpgSignatureKey(), Content.EMPTY).join();
        release.update(key).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Release file was updated",
            this.asto.value(new KeyFromPath("dists/my-deb/Release")).join().asString(),
            Matchers.allOf(
                new StringContainsInOrder(content),
                new StringContains(" 9751b63dcb589f0d84d20dcf5a0d347939c6f4f09d7911c40f330bfe6ffe686e 26 main/binary-intel/Packages.gz\n"),
                new StringContains(" 6ca13d52ca70c883e0f0bb101e425a89e8624de51db2d2392593af6a84118090 6 main/binary-intel/Packages\n")
            )
        );
        MatcherAssert.assertThat(
            "Gpg file was created if necessary",
            this.asto.exists(new KeyFromPath("dists/my-deb/Release.gpg")).join(),
            new IsEqual<>(gpg)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void updatesRecordInTheMiddle(final boolean gpg) throws IOException {
        this.asto.save(
            new Key.From("dists/my-repo/main/binary-amd64/Packages.gz"), Content.EMPTY
        ).join();
        final Key key = new Key.From("dists/my-repo/main/binary-intel/Packages.gz");
        new AstoGzArchive(this.asto).packAndSave("xyz", key);
        final ListOf<String> content = new ListOf<>(
            "Codename: my-repo",
            "Architectures: amd64 intel",
            "Components: main",
            "Date:",
            "SHA256:",
            " xyz098 2 main/binary-intel/Packages.gz",
            " abc123 4 main/binary-amd64/Packages.gz"
        );
        this.asto.save(
            new Key.From("dists/my-repo/Release"),
            new Content.From(String.join("\n", content).getBytes(StandardCharsets.UTF_8))
        ).join();
        new Release.Asto(
            this.asto,
            this.config(gpg, "my-repo", Yaml.createYamlMappingBuilder())
        ).update(key).toCompletableFuture().join();
        content.set(5, " eca44f5be15c27f009b837cf98df6a359304e868f024cfaff7f139baa6768d16 23 main/binary-intel/Packages.gz");
        content.add(" 3608bca1e44ea6c4d268eb6db02260269892c0b42b86bbf1e77a6fa16c3c9282 3 main/binary-intel/Packages\n");
        MatcherAssert.assertThat(
            "Release file was updated",
            this.asto.value(new KeyFromPath("dists/my-repo/Release")).join().asString(),
            new IsEqual<>(String.join("\n", content))
        );
        MatcherAssert.assertThat(
            "Gpg file was created if necessary",
            this.asto.exists(new KeyFromPath("dists/my-repo/Release.gpg")).join(),
            new IsEqual<>(gpg)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void updatesRecordAtTheEnd(final boolean gpg) throws IOException {
        this.asto.save(
            new Key.From("dists/deb-test/main/binary-amd64/Packages.gz"), Content.EMPTY
        ).join();
        final Key key = new Key.From("dists/deb-test/main/binary-intel/Packages.gz");
        new AstoGzArchive(this.asto).packAndSave("098", key);
        final ListOf<String> content = new ListOf<>(
            "Codename: deb-test",
            "Architectures: amd64 intel",
            "Components: main",
            "Date:",
            "SHA256:",
            " xyz098 2 main/binary-amd64/Packages.gz",
            " abc123 4 main/binary-intel/Packages.gz"
        );
        this.asto.save(
            new Key.From("dists/deb-test/Release"),
            new Content.From(String.join("\n", content).getBytes(StandardCharsets.UTF_8))
        ).join();
        new Release.Asto(
            this.asto,
            this.config(gpg, "deb-test", Yaml.createYamlMappingBuilder())
        ).update(key).toCompletableFuture().join();
        content.set(6, " 4a82f377b30e07bc43f712d4e5ac4783b9e53de23980753e121618357be09c3c 23 main/binary-intel/Packages.gz");
        content.add(" 35e1d1aeed3f7179b02a0dfde8f4e826e191649ee2acfd6da6b2ce7a12aa0f8b 3 main/binary-intel/Packages\n");
        MatcherAssert.assertThat(
            "Release file updated",
            this.asto.value(new KeyFromPath("dists/deb-test/Release")).join().asString(),
            new IsEqual<>(String.join("\n", content))
        );
        MatcherAssert.assertThat(
            "Gpg file was created if necessary",
            this.asto.exists(new KeyFromPath("dists/deb-test/Release.gpg")).join(),
            new IsEqual<>(gpg)
        );
    }

    @Test
    void returnsReleaseIndexKey() {
        MatcherAssert.assertThat(
            new Release.Asto(
                this.asto,
                new Config.FromYaml(
                    "deb-repo",
                    Optional.of(Yaml.createYamlMappingBuilder().build()),
                    new InMemoryStorage()
                )
            ).key(),
            new IsEqual<>(new Key.From("dists/deb-repo/Release"))
        );
    }

    @Test
    void returnsGpgReleaseIndexKey() {
        MatcherAssert.assertThat(
            new Release.Asto(
                this.asto,
                new Config.FromYaml(
                    "deb-repo",
                    Yaml.createYamlMappingBuilder().build(),
                    new InMemoryStorage()
                )
            ).gpgSignatureKey(),
            new IsEqual<>(new Key.From("dists/deb-repo/Release.gpg"))
        );
    }

    private Config config(final boolean gpg, final String name, final YamlMappingBuilder yaml) {
        final Storage settings = new InMemoryStorage();
        YamlMappingBuilder builder = yaml;
        if (gpg) {
            final String key = "secret-keys.gpg";
            builder = builder.add("gpg_password", "1q2w3e4r5t6y7u")
                .add("gpg_secret_key", key);
            new TestResource(key).saveTo(settings);
        }
        return new Config.FromYaml(name, builder.build(), settings);
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.maven.MetadataXml;
import com.artipie.maven.http.PutMetadataSlice;
import com.jcabi.matchers.XhtmlMatchers;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AstoMaven}.
 * @since 0.8
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class AstoMavenTest {

    /**
     * Logger upload key.
     */
    private static final Key LGR_UPLOAD = new Key.From(".update/com/test/logger");

    /**
     * Logger package key.
     */
    private static final Key LGR = new Key.From("com/test/logger");

    /**
     * Asto artifact key.
     */
    private static final Key.From ASTO = new Key.From("com/artipie/asto");

    /**
     * Asto upload key.
     */
    private static final Key.From ASTO_UPLOAD = new Key.From(".upload/com/artipie/asto");

    /**
     * Test storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void generatesMetadata() {
        final String latest = "0.20.2";
        this.addFilesToStorage(
            item -> !item.contains("1.0-SNAPSHOT") && !item.contains(latest),
            AstoMavenTest.ASTO
        );
        this.addFilesToStorage(
            item -> item.contains(latest),
            AstoMavenTest.ASTO_UPLOAD
        );
        this.metadataAndVersions(latest);
        new AstoMaven(this.storage).update(
            new Key.From(AstoMavenTest.ASTO_UPLOAD, latest), AstoMavenTest.ASTO
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Maven metadata xml is not correct",
            new XMLDocument(
                this.storage.value(new Key.From(AstoMavenTest.ASTO, "maven-metadata.xml"))
                    .thenCompose(content -> new PublisherAs(content).string(StandardCharsets.UTF_8))
                    .join()
            ),
            new AllOf<>(
                new ListOf<Matcher<? super XML>>(
                    // @checkstyle LineLengthCheck (20 lines)
                    XhtmlMatchers.hasXPath("/metadata/groupId[text() = 'com.artipie']"),
                    XhtmlMatchers.hasXPath("/metadata/artifactId[text() = 'asto']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/latest[text() = '0.20.2']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/release[text() = '0.20.2']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.15']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.11.1']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.20.1']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.20.2']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.18']"),
                    XhtmlMatchers.hasXPath("metadata/versioning/versions[count(//version) = 5]"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/lastUpdated")
                )
            )
        );
        MatcherAssert.assertThat(
            "Artifacts were not moved to the correct location",
            this.storage.list(new Key.From(AstoMavenTest.ASTO, latest)).join().size(),
            new IsEqual<>(3)
        );
        MatcherAssert.assertThat(
            "Upload directory was not cleaned up",
            this.storage.list(new Key.From(AstoMavenTest.ASTO_UPLOAD, latest))
                .join().size(),
            new IsEqual<>(0)
        );
    }

    @Test
    void generatesMetadataForFirstArtifact() {
        final String version = "1.0";
        new TestResource("maven-metadata.xml.example").saveTo(
            this.storage,
            new Key.From(
                AstoMavenTest.LGR_UPLOAD, version, PutMetadataSlice.SUB_META, "maven-metadata.xml"
            )
        );
        new AstoMaven(this.storage).update(
            new Key.From(AstoMavenTest.LGR_UPLOAD, version), AstoMavenTest.LGR
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Maven metadata xml is not correct",
            new XMLDocument(
                this.storage.value(new Key.From(AstoMavenTest.LGR, "maven-metadata.xml"))
                    .thenCompose(content -> new PublisherAs(content).string(StandardCharsets.UTF_8))
                    .join()
            ),
            new AllOf<>(
                new ListOf<Matcher<? super XML>>(
                    XhtmlMatchers.hasXPath("/metadata/groupId[text() = 'com.test']"),
                    XhtmlMatchers.hasXPath("/metadata/artifactId[text() = 'logger']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/latest[text() = '1.0']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/release[text() = '1.0']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '1.0']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions[count(//version) = 1]"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/lastUpdated")
                )
            )
        );
        MatcherAssert.assertThat(
            "Upload directory was not cleaned up",
            this.storage.list(new Key.From(AstoMavenTest.LGR_UPLOAD, version))
                .join().size(),
            new IsEqual<>(0)
        );
    }

    @Test
    void addsMetadataChecksums() {
        final String version = "0.1";
        new TestResource("maven-metadata.xml.example").saveTo(
            this.storage,
            new Key.From(
                AstoMavenTest.LGR_UPLOAD, version, PutMetadataSlice.SUB_META, "maven-metadata.xml"
            )
        );
        new AstoMaven(this.storage).update(
            new Key.From(AstoMavenTest.LGR_UPLOAD, version), AstoMavenTest.LGR
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.storage.list(AstoMavenTest.LGR).join().stream()
                .map(key -> new KeyLastPart(key).get())
                .filter(key -> key.contains("maven-metadata.xml"))
                .toArray(String[]::new),
            Matchers.arrayContainingInAnyOrder(
                "maven-metadata.xml", "maven-metadata.xml.sha1", "maven-metadata.xml.sha256",
                "maven-metadata.xml.sha512", "maven-metadata.xml.md5"
            )
        );
    }

    @Test
    void updatesCorrectlyWhenVersionIsDowngraded() {
        final String version = "1.0";
        new MetadataXml("com.test", "logger").addXmlToStorage(
            this.storage, new Key.From(AstoMavenTest.LGR, "maven-metadata.xml"),
            new MetadataXml.VersionTags("2.0", "2.0", new ListOf<>("2.0"))
        );
        this.storage.save(
            new Key.From(AstoMavenTest.LGR, "2.0", "logger-2.0.jar"), Content.EMPTY
        ).join();
        new MetadataXml("com.test", "logger").addXmlToStorage(
            this.storage,
            new Key.From(
                AstoMavenTest.LGR_UPLOAD, version, PutMetadataSlice.SUB_META, "maven-metadata.xml"
            ),
            new MetadataXml.VersionTags("2.0", "1.0", new ListOf<>("2.0", "1.0"))
        );
        new AstoMaven(this.storage).update(
            new Key.From(AstoMavenTest.LGR_UPLOAD, version), AstoMavenTest.LGR
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Maven metadata xml is not correct",
            new XMLDocument(
                this.storage.value(new Key.From(AstoMavenTest.LGR, "maven-metadata.xml"))
                    .thenCompose(content -> new PublisherAs(content).string(StandardCharsets.UTF_8))
                    .join()
            ),
            new AllOf<>(
                new ListOf<Matcher<? super XML>>(
                    XhtmlMatchers.hasXPath("/metadata/groupId[text() = 'com.test']"),
                    XhtmlMatchers.hasXPath("/metadata/artifactId[text() = 'logger']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/latest[text() = '2.0']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/release[text() = '2.0']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '2.0']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '1.0']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions[count(//version) = 2]"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/lastUpdated")
                )
            )
        );
        MatcherAssert.assertThat(
            "Upload directory was not cleaned up",
            this.storage.list(new Key.From(AstoMavenTest.LGR_UPLOAD, version))
                .join().size(),
            new IsEqual<>(0)
        );
    }

    @Test
    void generatesWithSnapshotMetadata() throws Exception {
        final String snapshot = "1.0-SNAPSHOT";
        final Predicate<String> cond = item -> !item.contains(snapshot);
        this.addFilesToStorage(cond, AstoMavenTest.ASTO);
        this.addFilesToStorage(cond.negate(), AstoMavenTest.ASTO_UPLOAD);
        this.metadataAndVersions(snapshot, "0.20.2");
        new AstoMaven(this.storage).update(
            new Key.From(AstoMavenTest.ASTO_UPLOAD, snapshot), AstoMavenTest.ASTO
        ).toCompletableFuture().get();
        MatcherAssert.assertThat(
            new XMLDocument(
                this.storage.value(new Key.From(AstoMavenTest.ASTO, "maven-metadata.xml"))
                    .thenCompose(content -> new PublisherAs(content).string(StandardCharsets.UTF_8))
                    .join()
            ),
            new AllOf<>(
                new ListOf<Matcher<? super XML>>(
                    // @checkstyle LineLengthCheck (20 lines)
                    XhtmlMatchers.hasXPath("/metadata/groupId[text() = 'com.artipie']"),
                    XhtmlMatchers.hasXPath("/metadata/artifactId[text() = 'asto']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/latest[text() = '1.0-SNAPSHOT']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/release[text() = '0.20.2']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.15']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.11.1']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.20.1']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.20.2']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '0.18']"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/versions/version[text() = '1.0-SNAPSHOT']"),
                    XhtmlMatchers.hasXPath("metadata/versioning/versions[count(//version) = 6]"),
                    XhtmlMatchers.hasXPath("/metadata/versioning/lastUpdated")
                )
            )
        );
        MatcherAssert.assertThat(
            "Artifacts were not moved to the correct location",
            this.storage.list(new Key.From(AstoMavenTest.ASTO, snapshot)).join().size(),
            new IsEqual<>(12)
        );
        MatcherAssert.assertThat(
            "Upload directory was not cleaned up",
            this.storage.list(new Key.From(AstoMavenTest.ASTO_UPLOAD, snapshot))
                .join().size(),
            new IsEqual<>(0)
        );
    }

    private void addFilesToStorage(final Predicate<String> condition, final Key base) {
        final Storage resources = new FileStorage(new TestResource("com/artipie/asto").asPath());
        final BlockingStorage bsto = new BlockingStorage(resources);
        bsto.list(Key.ROOT).stream()
            .map(Key::string)
            .filter(condition)
            .forEach(
                item -> new BlockingStorage(this.storage).save(
                    new Key.From(base, item),
                    bsto.value(new Key.From(item))
                )
        );
    }

    private void metadataAndVersions(final String release, final String... versions) {
        new MetadataXml("com.artipie", "asto").addXmlToStorage(
            this.storage,
            new Key.From(
                AstoMavenTest.ASTO_UPLOAD, release, PutMetadataSlice.SUB_META, "maven-metadata.xml"
            ),
            new MetadataXml.VersionTags(
                Optional.empty(), Optional.of("0.20.2"),
                Stream.concat(
                    Stream.of("0.11.1", "0.15", "0.18", "0.20.1", release),
                    Stream.of(versions)
                ).collect(Collectors.toList())
            )
        );
    }
}

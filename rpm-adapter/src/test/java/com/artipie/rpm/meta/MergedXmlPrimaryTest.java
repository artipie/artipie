/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.asto.test.TestResource;
import com.artipie.rpm.Digest;
import com.artipie.rpm.TestRpm;
import com.artipie.rpm.pkg.FilePackage;
import com.artipie.rpm.pkg.FilePackageHeader;
import com.jcabi.matchers.XhtmlMatchers;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link MergedXmlPrimary}.
 * @since 1.5
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class MergedXmlPrimaryTest {

    @Test
    void addsRecords() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TestRpm.Libdeflt libdeflt = new TestRpm.Libdeflt();
        try (InputStream input = new TestResource("repodata/primary.xml.example").asInputStream()) {
            final MergedXmlPrimary.Result res =
                new MergedXmlPrimary(input, out).merge(
                    new ListOf<>(
                        new FilePackage.Headers(
                            new FilePackageHeader(libdeflt.path()).header(),
                            libdeflt.path(), Digest.SHA256, libdeflt.path().getFileName().toString()
                        )
                    ),
                    new XmlEventPrimary()
                );
            MatcherAssert.assertThat(
                "Packages count is incorrect",
                res.count(),
                new IsEqual<>(3L)
            );
            MatcherAssert.assertThat(
                "Duplicated packages checksum should be empty",
                res.checksums(),
                new IsEmptyCollection<>()
            );
            MatcherAssert.assertThat(
                "Primary does not have expected packages",
                out.toString(StandardCharsets.UTF_8.name()),
                XhtmlMatchers.hasXPaths(
                    // @checkstyle LineLengthCheck (3 lines)
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='aom']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='nginx']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='libdeflt1_0']"
                )
            );
        }
    }

    @Test
    void addsReplacesRecords() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TestRpm.Libdeflt libdeflt = new TestRpm.Libdeflt();
        final TestRpm.Time time = new TestRpm.Time();
        try (InputStream input =
            new TestResource("repodata/MergedXmlTest/libdeflt-primary.xml.example").asInputStream()
        ) {
            final MergedXmlPrimary.Result res =
                new MergedXmlPrimary(input, out).merge(
                    new ListOf<>(
                        new FilePackage.Headers(
                            new FilePackageHeader(time.path()).header(),
                            time.path(), Digest.SHA256, time.path().getFileName().toString()
                        ),
                        new FilePackage.Headers(
                            new FilePackageHeader(libdeflt.path()).header(),
                            libdeflt.path(), Digest.SHA256, libdeflt.path().getFileName().toString()
                        )
                    ),
                    new XmlEventPrimary()
                );
            MatcherAssert.assertThat(
                "Packages count is incorrect",
                res.count(),
                new IsEqual<>(2L)
            );
            MatcherAssert.assertThat(
                "Duplicated packages checksum should contain one checksum",
                res.checksums(),
                Matchers.contains("abc123")
            );
            final String actual = out.toString(StandardCharsets.UTF_8.name());
            MatcherAssert.assertThat(
                "Primary does not have expected packages",
                actual,
                XhtmlMatchers.hasXPaths(
                    // @checkstyle LineLengthCheck (3 lines)
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='time']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='libdeflt1_0']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='checksum' and text()='47bbb8b2401e8853812e6340f4197252b92463c132f64a257e18c0c8c83ae462']"
                )
            );
        }
    }

    @Test
    void appendsSeveralPackages() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TestRpm libdeflt = new TestRpm.Libdeflt();
        final TestRpm time = new TestRpm.Time();
        final TestRpm abc = new TestRpm.Abc();
        try (InputStream input =
            new TestResource("repodata/MergedXmlTest/libdeflt-nginx-promary.xml.example")
                .asInputStream()
        ) {
            final MergedXmlPrimary.Result res =
                new MergedXmlPrimary(input, out).merge(
                    new ListOf<>(
                        new FilePackage.Headers(
                            new FilePackageHeader(time.path()).header(),
                            time.path(), Digest.SHA256, time.path().getFileName().toString()
                        ),
                        new FilePackage.Headers(
                            new FilePackageHeader(abc.path()).header(),
                            abc.path(), Digest.SHA256, abc.path().getFileName().toString()
                        ),
                        new FilePackage.Headers(
                            new FilePackageHeader(libdeflt.path()).header(),
                            libdeflt.path(), Digest.SHA256, libdeflt.path().getFileName().toString()
                        )
                    ),
                    new XmlEventPrimary()
                );
            MatcherAssert.assertThat(
                "Packages count is incorrect",
                res.count(),
                new IsEqual<>(4L)
            );
            MatcherAssert.assertThat(
                "Duplicated packages checksum should contain one checksum",
                res.checksums(),
                Matchers.contains("abc123")
            );
            final String actual = out.toString(StandardCharsets.UTF_8.name());
            MatcherAssert.assertThat(
                "Primary does not have expected packages",
                actual,
                XhtmlMatchers.hasXPaths(
                    // @checkstyle LineLengthCheck (5 lines)
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='time']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='abc']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='nginx']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='libdeflt1_0']",
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='checksum' and text()='47bbb8b2401e8853812e6340f4197252b92463c132f64a257e18c0c8c83ae462']"
                )
            );
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"primary_1.xml.example", "primary_2.xml.example"})
    void worksWithEmptyInput(final String filename) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TestRpm abc = new TestRpm.Abc();
        try (
            InputStream input = new TestResource(String.format("repodata/empty/%s", filename))
                .asInputStream()
        ) {
            final MergedXmlPrimary.Result res = new MergedXmlPrimary(input, out).merge(
                new ListOf<>(
                    new FilePackage.Headers(
                        new FilePackageHeader(abc.path()).header(),
                        abc.path(), Digest.SHA256, abc.path().getFileName().toString()
                    )
                ),
                new XmlEventPrimary()
            );
            MatcherAssert.assertThat(
                "Packages count is incorrect",
                res.count(),
                new IsEqual<>(1L)
            );
            MatcherAssert.assertThat(
                "Duplicated packages is not empty",
                res.checksums(),
                Matchers.emptyIterable()
            );
            final String actual = out.toString(StandardCharsets.UTF_8.name());
            MatcherAssert.assertThat(
                actual,
                XhtmlMatchers.hasXPaths(
                    // @checkstyle LineLengthCheck (1 line)
                    "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='abc']"
                )
            );
        }
    }

    @Test
    void worksWithAbsentInput() throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TestRpm time = new TestRpm.Time();
        final TestRpm abc = new TestRpm.Abc();
        final MergedXmlPrimary.Result res = new MergedXmlPrimary(Optional.empty(), out).merge(
            new ListOf<>(
                new FilePackage.Headers(
                    new FilePackageHeader(abc.path()).header(),
                    abc.path(), Digest.SHA256, abc.path().getFileName().toString()
                ),
                new FilePackage.Headers(
                    new FilePackageHeader(time.path()).header(),
                    time.path(), Digest.SHA256, time.path().getFileName().toString()
                )
            ),
            new XmlEventPrimary()
        );
        MatcherAssert.assertThat(
            "Packages count is incorrect",
            res.count(),
            new IsEqual<>(2L)
        );
        MatcherAssert.assertThat(
            "Duplicated packages is not empty",
            res.checksums(),
            Matchers.emptyIterable()
        );
        MatcherAssert.assertThat(
            "Primary does not have expected packages",
            out.toString(StandardCharsets.UTF_8.name()),
            XhtmlMatchers.hasXPaths(
                // @checkstyle LineLengthCheck (5 lines)
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='time']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='abc']"
            )
        );
    }
}

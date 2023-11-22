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
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link MergedXmlPackage}.
 * @since 1.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class MergedXmlPackageTest {

    @ParameterizedTest
    @ValueSource(strings = {"other", "filelists"})
    void addsRecords(final String filename) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TestRpm.Libdeflt libdeflt = new TestRpm.Libdeflt();
        try (InputStream input =
            new TestResource(String.format("repodata/%s.xml.example", filename)).asInputStream()) {
            final XmlPackage type = XmlPackage.valueOf(filename.toUpperCase(Locale.US));
            new MergedXmlPackage(
                input, out, type,
                new MergedXmlPrimary.Result(3L, Collections.emptyList())
            ).merge(
                new ListOf<>(
                    new FilePackage.Headers(
                        new FilePackageHeader(libdeflt.path()).header(),
                        libdeflt.path(), Digest.SHA256, libdeflt.path().getFileName().toString()
                    )
                ),
                this.event(type)
            );
            final String actual = out.toString(StandardCharsets.UTF_8.name());
            MatcherAssert.assertThat(
                actual,
                XhtmlMatchers.hasXPaths(
                    // @checkstyle LineLengthCheck (4 lines)
                    String.format("/*[local-name()='%s' and @packages='3']", type.tag()),
                    String.format("/*[local-name()='%s']/*[local-name()='package' and @name='aom']", type.tag()),
                    String.format("/*[local-name()='%s']/*[local-name()='package' and @name='nginx']", type.tag()),
                    String.format("/*[local-name()='%s']/*[local-name()='package' and @name='libdeflt1_0']", type.tag())
                )
            );
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"other", "filelists"})
    void replacesAndAddsRecord(final String filename) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TestRpm libdeflt = new TestRpm.Libdeflt();
        final TestRpm time = new TestRpm.Time();
        try (InputStream input = new TestResource(
            String.format("repodata/MergedXmlTest/libdeflt-%s.xml.example", filename)
        ).asInputStream()
        ) {
            final XmlPackage type = XmlPackage.valueOf(filename.toUpperCase(Locale.US));
            new MergedXmlPackage(
                input, out, type,
                new MergedXmlPrimary.Result(2L, Collections.singleton("abc123"))
            ).merge(
                new ListOf<>(
                    new FilePackage.Headers(
                        new FilePackageHeader(libdeflt.path()).header(),
                        libdeflt.path(), Digest.SHA256, libdeflt.path().getFileName().toString()
                    ),
                    new FilePackage.Headers(
                        new FilePackageHeader(time.path()).header(),
                        time.path(), Digest.SHA256, time.path().getFileName().toString()
                    )
                ),
                this.event(type)
            );
            final String actual = out.toString(StandardCharsets.UTF_8.name());
            MatcherAssert.assertThat(
                actual,
                XhtmlMatchers.hasXPaths(
                    // @checkstyle LineLengthCheck (4 lines)
                    String.format("/*[local-name()='%s' and @packages='2']", type.tag()),
                    String.format("/*[local-name()='%s']/*[local-name()='package' and @name='libdeflt1_0' and @pkgid='47bbb8b2401e8853812e6340f4197252b92463c132f64a257e18c0c8c83ae462']", type.tag()),
                    String.format("/*[local-name()='%s']/*[local-name()='package' and @name='time']", type.tag())
                )
            );
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"other", "filelists"})
    void appendsSeveralPackages(final String filename) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TestRpm libdeflt = new TestRpm.Libdeflt();
        final TestRpm time = new TestRpm.Time();
        final TestRpm abc = new TestRpm.Abc();
        try (InputStream input = new TestResource(
            String.format("repodata/MergedXmlTest/libdeflt-nginx-%s.xml.example", filename)
        ).asInputStream()
        ) {
            final XmlPackage type = XmlPackage.valueOf(filename.toUpperCase(Locale.US));
            new MergedXmlPackage(
                input, out, type,
                new MergedXmlPrimary.Result(4L, Collections.singleton("abc123"))
            ).merge(
                new ListOf<>(
                    new FilePackage.Headers(
                        new FilePackageHeader(libdeflt.path()).header(),
                        libdeflt.path(), Digest.SHA256, libdeflt.path().getFileName().toString()
                    ),
                    new FilePackage.Headers(
                        new FilePackageHeader(time.path()).header(),
                        time.path(), Digest.SHA256, time.path().getFileName().toString()
                    ),
                    new FilePackage.Headers(
                        new FilePackageHeader(abc.path()).header(),
                        abc.path(), Digest.SHA256, abc.path().getFileName().toString()
                    )
                ),
                this.event(type)
            );
            final String actual = out.toString(StandardCharsets.UTF_8.name());
            MatcherAssert.assertThat(
                actual,
                XhtmlMatchers.hasXPaths(
                    // @checkstyle LineLengthCheck (4 lines)
                    String.format("/*[local-name()='%s' and @packages='4']", type.tag()),
                    String.format("/*[local-name()='%s']/*[local-name()='package' and @name='libdeflt1_0' and @pkgid='47bbb8b2401e8853812e6340f4197252b92463c132f64a257e18c0c8c83ae462']", type.tag()),
                    String.format("/*[local-name()='%s']/*[local-name()='package' and @name='nginx']", type.tag()),
                    String.format("/*[local-name()='%s']/*[local-name()='package' and @name='abc']", type.tag())
                )
            );
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "other_1.xml.example", "other_2.xml.example",
        "filelists_1.xml.example", "filelists_2.xml.example"
    })
    void worksWithEmptyInput(final String filename) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TestRpm abc = new TestRpm.Abc();
        try (
            InputStream input = new TestResource(String.format("repodata/empty/%s", filename))
                .asInputStream()
        ) {
            final XmlPackage type = XmlPackage.valueOf(
                filename.substring(0, filename.indexOf('_')).toUpperCase(Locale.US)
            );
            new MergedXmlPackage(
                input, out, type,
                new MergedXmlPrimary.Result(1L, Collections.emptyList())
            ).merge(
                new ListOf<>(
                    new FilePackage.Headers(
                        new FilePackageHeader(abc.path()).header(),
                        abc.path(), Digest.SHA256, abc.path().getFileName().toString()
                    )
                ),
                this.event(type)
            );
            final String actual = out.toString(StandardCharsets.UTF_8.name());
            MatcherAssert.assertThat(
                actual,
                XhtmlMatchers.hasXPaths(
                    // @checkstyle LineLengthCheck (4 lines)
                    String.format("/*[local-name()='%s' and @packages='1']", type.tag()),
                    String.format("/*[local-name()='%s']/*[local-name()='package' and @name='abc']", type.tag())
                )
            );
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"other", "filelists"})
    void worksWithAbsentInput(final String filename) throws IOException {
        final XmlPackage type = XmlPackage.valueOf(filename.toUpperCase(Locale.US));
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final TestRpm time = new TestRpm.Time();
        final TestRpm abc = new TestRpm.Abc();
        new MergedXmlPackage(
            Optional.empty(), out, type,
            new MergedXmlPrimary.Result(2L, Collections.emptyList())
        ).merge(
            new ListOf<>(
                new FilePackage.Headers(
                    new FilePackageHeader(time.path()).header(),
                    time.path(), Digest.SHA256, time.path().getFileName().toString()
                ),
                new FilePackage.Headers(
                    new FilePackageHeader(abc.path()).header(),
                    abc.path(), Digest.SHA256, abc.path().getFileName().toString()
                )
            ),
            this.event(type)
        );
        MatcherAssert.assertThat(
            out.toString(StandardCharsets.UTF_8.name()),
            XhtmlMatchers.hasXPaths(
                // @checkstyle LineLengthCheck (3 lines)
                String.format("/*[local-name()='%s' and @packages='2']", type.tag()),
                String.format("/*[local-name()='%s']/*[local-name()='package' and @name='abc']", type.tag()),
                String.format("/*[local-name()='%s']/*[local-name()='package' and @name='time']", type.tag())
            )
        );
    }

    private XmlEvent event(final XmlPackage xml) {
        final XmlEvent res;
        if (xml == XmlPackage.OTHER) {
            res = new XmlEvent.Other();
        } else {
            res = new XmlEvent.Filelists();
        }
        return res;
    }
}

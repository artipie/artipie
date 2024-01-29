/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm;

import com.artipie.asto.test.TestResource;
import com.artipie.rpm.hm.IsXmlEqual;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.pkg.FilePackage;
import com.artipie.rpm.pkg.FilePackageHeader;
import com.jcabi.matchers.XhtmlMatchers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RpmMetadata.Append}.
 * @since 1.5
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class RpmMetadataAppendTest {

    @Test
    void appendsRecords() throws IOException {
        final ByteArrayOutputStream primary = new ByteArrayOutputStream();
        final ByteArrayOutputStream other = new ByteArrayOutputStream();
        final TestRpm.Libdeflt libdeflt = new TestRpm.Libdeflt();
        final TestRpm.Abc abc = new TestRpm.Abc();
        new RpmMetadata.Append(
            new RpmMetadata.MetadataItem(
                XmlPackage.PRIMARY,
                new ByteArrayInputStream(
                    new TestResource("repodata/primary.xml.example").asBytes()
                ),
                primary
            ),
            new RpmMetadata.MetadataItem(
                XmlPackage.OTHER,
                new ByteArrayInputStream(
                    new TestResource("repodata/other.xml.example").asBytes()
                ),
                other
            )
        ).perform(
            new ListOf<>(
                new FilePackage.Headers(
                    new FilePackageHeader(libdeflt.path()).header(),
                    libdeflt.path(), Digest.SHA256, libdeflt.path().getFileName().toString()
                ),
                new FilePackage.Headers(
                    new FilePackageHeader(abc.path()).header(),
                    abc.path(), Digest.SHA256, abc.path().getFileName().toString()
                )
            )
        );
        MatcherAssert.assertThat(
            "Records were not added to primary xml",
            primary.toString(),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='metadata' and @packages='4']",
                //@checkstyle LineLengthCheck (4 lines)
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='nginx']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='aom']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='abc']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='libdeflt1_0']"
            )
        );
        MatcherAssert.assertThat(
            "Records were not added to others xml",
            other.toString(),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='otherdata' and @packages='4']",
                "/*[local-name()='otherdata']/*[local-name()='package' and @name='aom']",
                "/*[local-name()='otherdata']/*[local-name()='package' and @name='nginx']",
                "/*[local-name()='otherdata']/*[local-name()='package' and @name='abc']",
                "/*[local-name()='otherdata']/*[local-name()='package' and @name='libdeflt1_0']"
            )
        );
    }

    @Test
    void createsIndexesWhenInputsAreAbsent() throws IOException {
        final ByteArrayOutputStream primary = new ByteArrayOutputStream();
        final ByteArrayOutputStream other = new ByteArrayOutputStream();
        final TestRpm.Time time = new TestRpm.Time();
        final TestRpm.Abc abc = new TestRpm.Abc();
        new RpmMetadata.Append(
            new RpmMetadata.MetadataItem(
                XmlPackage.PRIMARY,
                primary
            ),
            new RpmMetadata.MetadataItem(
                XmlPackage.OTHER,
                other
            )
        ).perform(
            new ListOf<>(
                new FilePackage.Headers(
                    new FilePackageHeader(time.path()).header(),
                    time.path(), Digest.SHA256, time.path().getFileName().toString()
                ),
                new FilePackage.Headers(
                    new FilePackageHeader(abc.path()).header(),
                    abc.path(), Digest.SHA256, abc.path().getFileName().toString()
                )
            )
        );
        MatcherAssert.assertThat(
            "Records were not added to primary xml",
            primary.toString(),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='metadata' and @packages='2']",
                //@checkstyle LineLengthCheck (2 lines)
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='time']",
                "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='abc']"
            )
        );
        MatcherAssert.assertThat(
            "Records were not added to others xml",
            other.toString(),
            XhtmlMatchers.hasXPaths(
                "/*[local-name()='otherdata' and @packages='2']",
                "/*[local-name()='otherdata']/*[local-name()='package' and @name='time']",
                "/*[local-name()='otherdata']/*[local-name()='package' and @name='abc']"
            )
        );
    }

    @Test
    void doesNothingWhenRmpsListIsEmpty() throws IOException {
        final ByteArrayOutputStream primary = new ByteArrayOutputStream();
        final ByteArrayOutputStream other = new ByteArrayOutputStream();
        final ByteArrayOutputStream filelists = new ByteArrayOutputStream();
        try (
            InputStream isfilelists = new TestResource("repodata/filelists.xml.example")
                .asInputStream();
            InputStream isprim = new TestResource("repodata/primary.xml.example").asInputStream();
            InputStream isother = new TestResource("repodata/other.xml.example").asInputStream()
        ) {
            new RpmMetadata.Append(
                new RpmMetadata.MetadataItem(XmlPackage.PRIMARY, isprim, primary),
                new RpmMetadata.MetadataItem(XmlPackage.OTHER, isother, other),
                new RpmMetadata.MetadataItem(XmlPackage.FILELISTS, isfilelists, filelists)
            ).perform(Collections.emptyList());
        }
        MatcherAssert.assertThat(
            "Records were changed in primary xml",
            new TestResource("repodata/primary.xml.example").asPath(),
            new IsXmlEqual(primary.toByteArray())
        );
        MatcherAssert.assertThat(
            "Records were changed in others xml",
            new TestResource("repodata/other.xml.example").asPath(),
            new IsXmlEqual(other.toByteArray())
        );
        MatcherAssert.assertThat(
            "Records were changed in filelists xml",
            new TestResource("repodata/filelists.xml.example").asPath(),
            new IsXmlEqual(filelists.toByteArray())
        );
    }

    @Test
    void generatesEmptyIndexesWhenRpmsListIsEmpty() {
        final ByteArrayOutputStream primary = new ByteArrayOutputStream();
        final ByteArrayOutputStream other = new ByteArrayOutputStream();
        final ByteArrayOutputStream filelists = new ByteArrayOutputStream();
        new RpmMetadata.Append(
            new RpmMetadata.MetadataItem(XmlPackage.PRIMARY, primary),
            new RpmMetadata.MetadataItem(XmlPackage.OTHER, other),
            new RpmMetadata.MetadataItem(XmlPackage.FILELISTS, filelists)
        ).perform(Collections.emptyList());
        MatcherAssert.assertThat(
            "Empty primary xml was not generated",
            primary.toString(),
            XhtmlMatchers.hasXPaths("/*[local-name()='metadata' and @packages='0']")
        );
        MatcherAssert.assertThat(
            "Empty other xml was not generated",
            other.toString(),
            XhtmlMatchers.hasXPaths("/*[local-name()='otherdata' and @packages='0']")
        );
        MatcherAssert.assertThat(
            "Empty filelists xml was not generated",
            filelists.toString(),
            XhtmlMatchers.hasXPaths("/*[local-name()='filelists' and @packages='0']")
        );
    }
}

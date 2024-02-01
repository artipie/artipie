/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm;

import com.artipie.asto.test.TestResource;
import com.artipie.rpm.hm.IsXmlEqual;
import com.artipie.rpm.meta.XmlPackage;
import com.jcabi.matchers.XhtmlMatchers;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.cactoos.list.ListOf;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link RpmMetadata.Remove}.
 * @since 1.4
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class RpmMetadataRemoveTest {

    @Test
    void removesRecord() throws IOException {
        final ByteArrayOutputStream primary = new ByteArrayOutputStream();
        final ByteArrayOutputStream filelist = new ByteArrayOutputStream();
        final String checksum = "7eaefd1cb4f9740558da7f12f9cb5a6141a47f5d064a98d46c29959869af1a44";
        new RpmMetadata.Remove(
            new RpmMetadata.MetadataItem(
                XmlPackage.PRIMARY,
                new ByteArrayInputStream(
                    new TestResource("repodata/primary.xml.example").asBytes()
                ),
                primary
            ),
            new RpmMetadata.MetadataItem(
                XmlPackage.FILELISTS,
                new ByteArrayInputStream(
                    new TestResource("repodata/filelists.xml.example").asBytes()
                ),
                filelist
            )
        ).perform(new ListOf<>(checksum));
        MatcherAssert.assertThat(
            "Record was not removed from primary xml",
            primary.toString(),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    XhtmlMatchers.hasXPaths(
                        "/*[local-name()='metadata' and @packages='1']",
                        "/*[local-name()='metadata']/*[local-name()='package']/*[local-name()='name' and text()='nginx']"
                    ),
                    new IsNot<>(new StringContains(checksum))
                )
            )
        );
        MatcherAssert.assertThat(
            "Record was not removed from filelist xml",
            filelist.toString(),
            new AllOf<>(
                new ListOf<Matcher<? super String>>(
                    XhtmlMatchers.hasXPaths(
                        "/*[local-name()='filelists' and @packages='1']",
                        "/*[local-name()='filelists']/*[local-name()='package' and @name='nginx']"
                    ),
                    new IsNot<>(new StringContains(checksum))
                )
            )
        );
    }

    @ParameterizedTest
    @CsvSource({
        "filelists_1.xml.example,other_1.xml.example,primary_1.xml.example",
        "filelists_2.xml.example,other_2.xml.example,primary_2.xml.example"
    })
    void doesNothingIfIndexIsEmpty(final String ilist, final String iother, final String iprim) {
        final ByteArrayOutputStream primary = new ByteArrayOutputStream();
        final ByteArrayOutputStream other = new ByteArrayOutputStream();
        final ByteArrayOutputStream filelists = new ByteArrayOutputStream();
        new RpmMetadata.Remove(
            new RpmMetadata.MetadataItem(
                XmlPackage.PRIMARY,
                new ByteArrayInputStream(
                    new TestResource(String.format("repodata/empty/%s", iprim)).asBytes()
                ),
                primary
            ),
            new RpmMetadata.MetadataItem(
                XmlPackage.OTHER,
                new ByteArrayInputStream(
                    new TestResource(String.format("repodata/empty/%s", iother)).asBytes()
                ),
                other
            ),
            new RpmMetadata.MetadataItem(
                XmlPackage.FILELISTS,
                new ByteArrayInputStream(
                    new TestResource(String.format("repodata/empty/%s", ilist)).asBytes()
                ),
                filelists
            )
        ).perform(new ListOf<>("abc123"));
        MatcherAssert.assertThat(
            "Primary xml is not the same",
            new TestResource(String.format("repodata/empty/%s", iprim)).asPath(),
            new IsXmlEqual(primary.toByteArray())
        );
        MatcherAssert.assertThat(
            "Other xml is not the same",
            new TestResource(String.format("repodata/empty/%s", iother)).asPath(),
            new IsXmlEqual(other.toByteArray())
        );
        MatcherAssert.assertThat(
            "Filelists xml is not the same",
            new TestResource(String.format("repodata/empty/%s", ilist)).asPath(),
            new IsXmlEqual(filelists.toByteArray())
        );
    }
}

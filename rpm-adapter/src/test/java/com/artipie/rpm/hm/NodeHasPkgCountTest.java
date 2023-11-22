/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.hm;

import com.artipie.asto.test.TestResource;
import com.artipie.rpm.meta.XmlPackage;
import com.jcabi.xml.XMLDocument;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;
import org.llorllale.cactoos.matchers.Matches;
import org.llorllale.cactoos.matchers.Mismatches;

/**
 * Test for {@link NodeHasPkgCount}.
 * @since 0.10
 * @checkstyle MagicNumberCheck (500 lines)
 */
final class NodeHasPkgCountTest {

    /**
     * Wrong xml path.
     */
    private static final Path WRONG =
        new TestResource("repodata/wrong-package.xml.example").asPath();

    /**
     * Primary xml example path.
     */
    private static final Path PRIMARY =
        new TestResource("repodata/primary.xml.example").asPath();

    @Test
    void countsPackages() throws FileNotFoundException {
        MatcherAssert.assertThat(
            new NodeHasPkgCount(2, XmlPackage.OTHER.tag()),
            new Matches<>(
                new XMLDocument(
                    new TestResource("repodata/other.xml.example").asPath()
                )
            )
        );
    }

    @Test
    void doesNotMatchWhenPackagesAmountDiffers() throws FileNotFoundException {
        MatcherAssert.assertThat(
            new NodeHasPkgCount(10, XmlPackage.PRIMARY.tag()),
            new IsNot<>(
                new Matches<>(
                    new XMLDocument(NodeHasPkgCountTest.PRIMARY)
                )
            )
        );
    }

    @Test
    void describesCorrectlyWhenPackagesAmountDiffers() throws FileNotFoundException {
        MatcherAssert.assertThat(
            new NodeHasPkgCount(10, XmlPackage.PRIMARY.tag()),
            new Mismatches<>(
                new XMLDocument(NodeHasPkgCountTest.PRIMARY),
                "10 packages expected",
                "2 packages found, 'packages' attribute value is 2"
            )
        );
    }

    @Test
    void doesNotMatchWhenPackageAttributeDiffers() throws FileNotFoundException {
        MatcherAssert.assertThat(
            new NodeHasPkgCount(2, XmlPackage.OTHER.tag()),
            new IsNot<>(
                new Matches<>(
                    new XMLDocument(NodeHasPkgCountTest.WRONG)
                )
            )
        );
    }

    @Test
    void describesCorrectlyWhenPackageAttributeDiffers() throws FileNotFoundException {
        MatcherAssert.assertThat(
            new NodeHasPkgCount(2, XmlPackage.OTHER.tag()),
            new Mismatches<>(
                new XMLDocument(NodeHasPkgCountTest.WRONG),
                "2 packages expected",
                "2 packages found, 'packages' attribute value is 3"
            )
        );
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.asto.test.TestResource;
import com.artipie.rpm.hm.IsXmlEqual;
import com.jcabi.matchers.XhtmlMatchers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test {@link XmlAlter}.
 * @since 0.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class XmlAlterTest {

    @Test
    public void writesCorrectPackageCount(@TempDir final Path temp) throws Exception {
        final Path file = temp.resolve("primary.xml");
        Files.copy(new TestResource("repodata/primary.xml.example").asPath(), file);
        final int expected = 10;
        new XmlAlter.File(file).pkgAttr("metadata", String.valueOf(expected));
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(file), StandardCharsets.UTF_8),
            XhtmlMatchers.hasXPath(String.format("/*[@packages='%s']", expected))
        );
    }

    @Test
    public void writesPackageCountToNotProperlyFormattedXml(@TempDir final Path temp)
        throws Exception {
        final Path file = temp.resolve("test.xml");
        Files.write(
            file,
            String.join(
                "\n",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<tag packages=\"2\" name=\"abc\"><a>2</a></tag>"
            ).getBytes()
        );
        final int expected = 10;
        new XmlAlter.File(file).pkgAttr("tag", String.valueOf(expected));
        MatcherAssert.assertThat(
            new String(Files.readAllBytes(file), StandardCharsets.UTF_8),
            XhtmlMatchers.hasXPath(String.format("/*[@packages='%s']", expected))
        );
    }

    @Test
    public void doesNothingIfTagNotFound(@TempDir final Path temp) throws Exception {
        final Path file = temp.resolve("one.xml");
        final byte[] xml = String.join(
            "\n",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<one packages=\"2\" name=\"abc\"><a>2</a></one>"
        ).getBytes();
        Files.write(file, xml);
        new XmlAlter.File(file).pkgAttr("two", "10");
        MatcherAssert.assertThat(
            file,
            new IsXmlEqual(xml)
        );
    }

    @Test
    public void doesNothingIfAttrNotFound(@TempDir final Path temp) throws Exception {
        final Path file = temp.resolve("one.xml");
        final byte[] xml = String.join(
            "\n",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<tag name=\"abc\"><a>2</a></tag>"
        ).getBytes();
        Files.write(file, xml);
        new XmlAlter.File(file).pkgAttr("tag", "23");
        MatcherAssert.assertThat(
            file,
            new IsXmlEqual(xml)
        );
    }

}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.asto.test.TestResource;
import com.artipie.rpm.Digest;
import com.artipie.rpm.hm.IsXmlEqual;
import com.artipie.rpm.pkg.FilePackage;
import com.artipie.rpm.pkg.FilePackageHeader;
import com.fasterxml.aalto.stax.OutputFactoryImpl;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link XmlEvent.Other}.
 * @since 1.5
 */
class XmlEventOtherTest {

    /**
     * Temporary directory for all tests.
     */
    @TempDir
    Path tmp;

    @Test
    void writesPackageInfo() throws XMLStreamException, IOException {
        final Path res = Files.createTempFile(this.tmp, "others", ".xml");
        final Path file = new TestResource("abc-1.01-26.git20200127.fc32.ppc64le.rpm").asPath();
        try (OutputStream out = Files.newOutputStream(res)) {
            final XMLEventWriter writer = new OutputFactoryImpl().createXMLEventWriter(out);
            new XmlEvent.Other().add(
                writer,
                new FilePackage.Headers(new FilePackageHeader(file).header(), file, Digest.SHA256)
            );
            writer.close();
        }
        MatcherAssert.assertThat(
            res,
            new IsXmlEqual(
                String.join(
                    "\n",
                    //@checkstyle LineLengthCheck (1 line)
                    "<package pkgid=\"b9d10ae3485a5c5f71f0afb1eaf682bfbea4ea667cc3c3975057d6e3d8f2e905\" name=\"abc\" arch=\"ppc64le\">",
                    "<version epoch=\"0\" ver=\"1.01\" rel=\"26.git20200127.fc32\"/>",
                    "</package>"
                )
            )
        );
    }

}

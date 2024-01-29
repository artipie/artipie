/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.asto.test.TestResource;
import com.artipie.rpm.Digest;
import com.artipie.rpm.pkg.FilePackage;
import com.artipie.rpm.pkg.FilePackageHeader;
import com.fasterxml.aalto.stax.OutputFactoryImpl;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.redline_rpm.header.Header;
import org.xmlunit.matchers.CompareMatcher;

/**
 * Test for {@link XmlEventPrimary}.
 * @since 1.5
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class XmlEventPrimaryTest {

    @ParameterizedTest
    @CsvSource({
        "abc-1.01-26.git20200127.fc32.ppc64le.rpm,abc_res.xml",
        "libnss-mymachines2-245-1.x86_64.rpm,libnss_res.xml",
        "openssh-server-7.4p1-16.h16.eulerosv2r7.x86_64.rpm,openssh_res.xml",
        "httpd-2.4.6-80.1.h8.eulerosv2r7.x86_64.rpm,httpd_res.xml",
        "felix-framework-4.2.1-5.el7.noarch.rpm,felix-framework-res.xml",
        "ant-1.9.4-2.el7.noarch.rpm,ant_res.xml",
        "dbus-1.6.12-17.el7.x86_64.rpm,dbus_res.xml",
        "compat-db47-4.7.25-28.el7.i686.rpm,compat_res.xml",
        "authconfig-6.2.8-30.el7.x86_64.rpm,authconfig_res.xml",
        "openssl-devel-1.0.2k-19.el7.x86_64.rpm,openssl-devil.xml",
        "nmap-7.80-1.h1.eulerosv2r9.x86_64.rpm,nmap_res.xml",
        "systemtap-client-4.1-6.eulerosv2r9.x86_64.rpm,systemtap_res.xml",
        "python3-pyasn1-0.3.7-8.eulerosv2r9.noarch.rpm,python.xml",
        "vim-base-7.2-8.15.2.x86_64.rpm,vim-base.xml",
        "apr-util-1.6.1-13.h1.eulerosv2r12.x86_64.rpm,apr-util.xml"
    })
    void writesPackageInfo(final String rpm, final String res) throws XMLStreamException,
        IOException {
        final Path file = new TestResource(rpm).asPath();
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final XMLEventWriter writer = new OutputFactoryImpl().createXMLEventWriter(bout);
        this.prepareXmlWriter(writer);
        new XmlEventPrimary().add(
            writer,
            new FilePackage.Headers(new FilePackageHeader(file).header(), file, Digest.SHA256)
        );
        writer.add(XMLEventFactory.newFactory().createEndElement("", "", "metadata"));
        writer.close();
        MatcherAssert.assertThat(
            bout.toByteArray(),
            CompareMatcher.isIdenticalTo(
                new TestResource(String.format("XmlEventPrimaryTest/%s", res)).asBytes()
            )
                .ignoreWhitespace()
                .ignoreElementContentWhitespace()
                .normalizeWhitespace()
        );
    }

    @Test
    void removesConflictDuplicates(final @TempDir Path tmp) throws XMLStreamException, IOException {
        final Path rpm = tmp.resolve("test.rpm");
        Files.write(rpm, "any".getBytes(StandardCharsets.UTF_8));
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final XMLEventWriter writer = new OutputFactoryImpl().createXMLEventWriter(bout);
        this.prepareXmlWriter(writer);
        final Header hdr = new Header();
        // @checkstyle LineLengthCheck (2 lines)
        hdr.createEntry(Header.HeaderTag.CONFLICTNAME, new String[]{"one", "two", "one", "three", "two"});
        hdr.createEntry(Header.HeaderTag.CONFLICTVERSION, new String[]{"0.1", "0.2", "0.1", "0.3", "0.2.2"});
        hdr.createEntry(Header.HeaderTag.CONFLICTFLAGS, new int[]{2, 8, 2, 2, 8});
        new XmlEventPrimary().add(writer, new FilePackage.Headers(hdr, rpm, Digest.SHA256));
        writer.add(XMLEventFactory.newFactory().createEndElement("", "", "metadata"));
        writer.close();
        MatcherAssert.assertThat(
            bout.toString(),
            new IsEqual<>(
                String.join(
                    "",
                    // @checkstyle LineLengthCheck (1 line)
                    "<?xml version='1.0' encoding='UTF-8'?><metadata xmlns=\"http://linux.duke.edu/metadata/common\" xmlns:rpm=\"http://linux.duke.edu/metadata/rpm\"><package type=\"rpm\"><name></name><arch></arch><version epoch=\"0\" rel=\"\" ver=\"\"/><checksum type=\"sha256\" pkgid=\"YES\">d6a7cd2a7371b1a15d543196979ff74fdb027023ebf187d5d329be11055c77fd</checksum><summary></summary><description></description><packager></packager><url></url><time file=\"0\" build=\"0\"/><size installed=\"0\" package=\"3\" archive=\"0\"/><location href=\"test.rpm\"/><format><rpm:license></rpm:license><rpm:vendor></rpm:vendor><rpm:group></rpm:group><rpm:buildhost></rpm:buildhost><rpm:sourcerpm></rpm:sourcerpm><rpm:header-range start=\"0\" end=\"0\"/><rpm:requires/>",
                    "<rpm:conflicts>",
                    "<rpm:entry name=\"one\" ver=\"0.1\" epoch=\"0\" flags=\"LT\"/>",
                    "<rpm:entry name=\"two\" ver=\"0.2\" epoch=\"0\" flags=\"EQ\"/>",
                    "<rpm:entry name=\"three\" ver=\"0.3\" epoch=\"0\" flags=\"LT\"/>",
                    "<rpm:entry name=\"two\" ver=\"0.2.2\" epoch=\"0\" flags=\"EQ\"/>",
                    "</rpm:conflicts>",
                    "</format></package></metadata>"
                )
            )
        );
    }

    @ParameterizedTest
    @CsvSource({
        "bash,/usr/bin/bash",
        "perl,/usr/bin/perl",
        "ruby,/usr/bin/ruby",
        "zsh,/bin/zsh",
        "python-debug,/usr/bin/python2-debug"
    })
    void excludesFilesFromRequires(final String name, final String requires,
        final @TempDir Path tmp) throws XMLStreamException, IOException {
        final Path rpm = tmp.resolve("test.rpm");
        Files.write(rpm, "any".getBytes(StandardCharsets.UTF_8));
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final XMLEventWriter writer = new OutputFactoryImpl().createXMLEventWriter(bout);
        this.prepareXmlWriter(writer);
        final Header hdr = new Header();
        hdr.createEntry(Header.HeaderTag.NAME, name);
        hdr.createEntry(Header.HeaderTag.BASENAMES, new String[]{"any", requires});
        hdr.createEntry(Header.HeaderTag.DIRNAMES, new String[]{"", ""});
        hdr.createEntry(Header.HeaderTag.DIRINDEXES, new int[]{0, 1});
        hdr.createEntry(Header.HeaderTag.FILEMODES, new short[]{0, 1});
        hdr.createEntry(Header.HeaderTag.FILEFLAGS, new int[]{0, 0});
        // @checkstyle LineLengthCheck (2 lines)
        hdr.createEntry(Header.HeaderTag.REQUIRENAME, new String[]{"one", "two", requires, "three"});
        hdr.createEntry(Header.HeaderTag.REQUIREVERSION, new String[]{"0.1", "0.2", "", "0.3.0"});
        hdr.createEntry(Header.HeaderTag.REQUIREFLAGS, new int[]{8, 8, 8, 8});
        new XmlEventPrimary().add(writer, new FilePackage.Headers(hdr, rpm, Digest.SHA256));
        writer.add(XMLEventFactory.newFactory().createEndElement("", "", "metadata"));
        writer.close();
        MatcherAssert.assertThat(
            bout.toString(),
            new IsEqual<>(
                String.join(
                    "",
                    // @checkstyle LineLengthCheck (5 lines)
                    "<?xml version='1.0' encoding='UTF-8'?><metadata xmlns=\"http://linux.duke.edu/metadata/common\" xmlns:rpm=\"http://linux.duke.edu/metadata/rpm\"><package type=\"rpm\">",
                    String.format("<name>%s</name>", name),
                    "<arch></arch><version epoch=\"0\" rel=\"\" ver=\"\"/><checksum type=\"sha256\" pkgid=\"YES\">d6a7cd2a7371b1a15d543196979ff74fdb027023ebf187d5d329be11055c77fd</checksum><summary></summary><description></description><packager></packager><url></url><time file=\"0\" build=\"0\"/><size installed=\"0\" package=\"3\" archive=\"0\"/><location href=\"test.rpm\"/><format><rpm:license></rpm:license><rpm:vendor></rpm:vendor><rpm:group></rpm:group><rpm:buildhost></rpm:buildhost><rpm:sourcerpm></rpm:sourcerpm><rpm:header-range start=\"0\" end=\"0\"/>",
                    "<rpm:requires>",
                    "<rpm:entry name=\"one\" ver=\"0.1\" epoch=\"0\" flags=\"EQ\"/>",
                    "<rpm:entry name=\"two\" ver=\"0.2\" epoch=\"0\" flags=\"EQ\"/>",
                    "<rpm:entry name=\"three\" ver=\"0.3.0\" epoch=\"0\" flags=\"EQ\"/>",
                    "</rpm:requires>",
                    String.format("<file>any</file><file>%s</file>", requires),
                    "</format></package></metadata>"
                )
            )
        );
    }

    private void prepareXmlWriter(final XMLEventWriter writer) throws XMLStreamException {
        final XMLEventFactory events = XMLEventFactory.newFactory();
        writer.add(events.createStartDocument());
        writer.add(events.createStartElement("", "", "metadata"));
        writer.add(events.createNamespace("http://linux.duke.edu/metadata/common"));
        writer.add(events.createNamespace("rpm", "http://linux.duke.edu/metadata/rpm"));
    }
}

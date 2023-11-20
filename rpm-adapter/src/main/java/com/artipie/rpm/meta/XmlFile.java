/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.meta;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Xml file.
 *
 * @since 1.0
 */
final class XmlFile extends XmlWriterWrap {

    /**
     * XML factory.
     */
    private static final XMLOutputFactory FACTORY = XMLOutputFactory.newInstance();

    /**
     * Output stream.
     */
    private final OutputStream stream;

    /**
     * Primary ctor.
     * @param path File path
     */
    XmlFile(final Path path) {
        this(outputStream(path));
    }

    /**
     * Primary ctor.
     * @param out Underlying output stream
     */
    XmlFile(final OutputStream out) {
        super(xmlStreamWriter(out));
        this.stream = out;
    }

    @Override
    public void close() throws XMLStreamException {
        try {
            super.close();
            this.stream.close();
        } catch (final IOException ex) {
            throw new XMLStreamException("Failed to close", ex);
        }
    }

    /**
     * New stream from path.
     * @param path File path
     * @return Output stream
     */
    private static OutputStream outputStream(final Path path) {
        try {
            return Files.newOutputStream(path);
        } catch (final IOException err) {
            throw new UncheckedIOException("Failed to open file stream", err);
        }
    }

    /**
     * New XML stream writer from path.
     * @param stream Output stream
     * @return XML stream writer
     */
    private static XMLStreamWriter xmlStreamWriter(final OutputStream stream) {
        try {
            return XmlFile.FACTORY.createXMLStreamWriter(
                stream,
                StandardCharsets.UTF_8.name()
            );
        } catch (final XMLStreamException err) {
            throw new XmlException("Failed to create XML stream", err);
        }
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.rpm.pkg.Checksum;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * XML {@code repomd.xml} metadata imperative writer.
 * <p>
 * This object is not thread safe and depends on order of method calls.
 * </p>
 * @since 0.6
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class XmlRepomd implements Closeable {

    /**
     * XML stream writer.
     */
    private final XmlFile xml;

    /**
     * Repomd path.
     */
    private final Path path;

    /**
     * Ctor.
     * @param path Repomd path
     */
    public XmlRepomd(final Path path) {
        this(path, new XmlFile(path));
    }

    /**
     * Ctor.
     * @param out Repomd output stream
     */
    public XmlRepomd(final OutputStream out) {
        this(Paths.get("any"), new XmlFile(out));
    }

    /**
     * Ctor.
     * @param path Repomd path
     * @param xml Xml writer
     */
    public XmlRepomd(final Path path, final XmlFile xml) {
        this.xml = xml;
        this.path = path;
    }

    /**
     * Begin repomd.
     * @param timestamp Current timestamp in seconds unix time.
     */
    public void begin(final long timestamp) {
        try {
            this.xml.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
            this.xml.writeStartElement("repomd");
            this.xml.writeDefaultNamespace("http://linux.duke.edu/metadata/repo");
            this.xml.writeStartElement("revision");
            this.xml.writeCharacters(String.valueOf(timestamp));
            this.xml.writeEndElement();
        } catch (final XMLStreamException ex) {
            throw new XmlException("Failed to start repomd", ex);
        }
    }

    /**
     * Start repomd data.
     * @param type Data type
     * @return Data writer
     * @throws XMLStreamException On error
     */
    public XmlRepomd.Data beginData(final String type) throws XMLStreamException {
        this.xml.writeStartElement("data");
        this.xml.writeAttribute("type", type);
        return new XmlRepomd.Data(this.xml);
    }

    /**
     * Repomd file.
     * @return File path
     */
    public Path file() {
        return this.path;
    }

    @Override
    public void close() {
        try {
            this.xml.writeEndElement();
            this.xml.close();
        } catch (final XMLStreamException err) {
            throw new XmlException("Failed to close", err);
        }
    }

    /**
     * Repomd {@code data} updater.
     * @since 0.6
     */
    public static final class Data implements Closeable {

        /**
         * XML stream writer.
         */
        private final XMLStreamWriter xml;

        /**
         * Ctor.
         * @param xml XML stream writer
         */
        private Data(final XMLStreamWriter xml) {
            this.xml = xml;
        }

        /**
         * Add checksum.
         * @param checksum Checksum
         * @throws XMLStreamException On error
         * @throws IOException On checksum error
         */
        public void gzipChecksum(final Checksum checksum) throws XMLStreamException, IOException {
            this.xml.writeStartElement("checksum");
            this.xml.writeAttribute("type", checksum.digest().type());
            this.xml.writeCharacters(checksum.hex());
            this.xml.writeEndElement();
        }

        /**
         * Add open-checksum.
         * @param checksum Checksum
         * @throws XMLStreamException On error
         * @throws IOException On checksum error
         */
        public void openChecksum(final Checksum checksum) throws XMLStreamException, IOException {
            this.xml.writeStartElement("open-checksum");
            this.xml.writeAttribute("type", checksum.digest().type());
            this.xml.writeCharacters(checksum.hex());
            this.xml.writeEndElement();
        }

        /**
         * Add location.
         * @param href Location href
         * @throws XMLStreamException On error
         */
        public void location(final String href) throws XMLStreamException {
            this.xml.writeEmptyElement("location");
            this.xml.writeAttribute("href", href);
        }

        /**
         * Add a timestamp.
         * @param sec Timestamp in seconds unix time
         * @throws XMLStreamException On error
         */
        public void timestamp(final long sec) throws XMLStreamException {
            this.xml.writeStartElement("timestamp");
            this.xml.writeCharacters(Long.toString(sec));
            this.xml.writeEndElement();
        }

        /**
         * Add gzip file size.
         * @param size Size in bytes
         * @throws XMLStreamException On error
         */
        public void gzipSize(final long size) throws XMLStreamException {
            this.xml.writeStartElement("size");
            this.xml.writeCharacters(Long.toString(size));
            this.xml.writeEndElement();
        }

        /**
         * Add open file size.
         * @param size Size in bytes
         * @throws XMLStreamException On error
         */
        public void openSize(final long size) throws XMLStreamException {
            this.xml.writeStartElement("open-size");
            this.xml.writeCharacters(Long.toString(size));
            this.xml.writeEndElement();
        }

        @Override
        public void close() {
            try {
                this.xml.writeEndElement();
            } catch (final XMLStreamException err) {
                throw new XmlException("Failed to close", err);
            }
        }
    }
}

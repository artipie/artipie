/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.rpm.RpmMetadata;
import com.artipie.rpm.pkg.Package;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Merged xml: reads provided index (filelist of others xml), excludes items by
 * provided checksums, adds items by provided file paths and updates `packages` attribute value.
 * @since 1.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle NestedTryDepthCheck (500 lines)
 */
public final class MergedXmlPackage implements MergedXml {

    /**
     * From where to read primary.xml.
     */
    private final Optional<InputStream> input;

    /**
     * Where to write the result.
     */
    private final OutputStream out;

    /**
     * Xml package type.
     */
    private final XmlPackage type;

    /**
     * Result of the primary.xml merging.
     */
    private final MergedXml.Result res;

    /**
     * Ctor.
     * @param input Input stream
     * @param out Output stream
     * @param type Xml package type
     * @param res Result of the primary.xml merging
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public MergedXmlPackage(final Optional<InputStream> input, final OutputStream out,
        final XmlPackage type, final MergedXml.Result res) {
        this.input = input;
        this.out = out;
        this.type = type;
        this.res = res;
    }

    /**
     * Ctor.
     * @param input Input stream
     * @param out Output stream
     * @param type Xml package type
     * @param res Result of the primary.xml merging
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public MergedXmlPackage(final InputStream input, final OutputStream out,
        final XmlPackage type, final MergedXml.Result res) {
        this(Optional.of(input), out, type, res);
    }

    @Override
    public MergedXml.Result merge(final Collection<Package.Meta> packages,
        final XmlEvent event) throws IOException {
        try {
            final XMLEventWriter writer = RpmMetadata.OUTPUT_FACTORY.createXMLEventWriter(this.out);
            try {
                MergedXmlPackage.startDocument(writer, String.valueOf(this.res.count()), this.type);
                if (this.input.isPresent()) {
                    final XMLEventReader reader = RpmMetadata.INPUT_FACTORY
                        .createXMLEventReader(this.input.get());
                    try {
                        this.process(this.res.checksums(), reader, writer);
                    } finally {
                        reader.close();
                    }
                }
                for (final Package.Meta item : packages) {
                    event.add(writer, item);
                }
                writer.add(RpmMetadata.EVENTS_FACTORY.createSpace("\n"));
                writer.add(
                    RpmMetadata.EVENTS_FACTORY.createEndElement(
                        new QName(this.type.tag()),
                        Collections.emptyIterator()
                    )
                );
            } finally {
                writer.close();
            }
        } catch (final XMLStreamException err) {
            throw new IOException(err);
        }
        return this.res;
    }

    /**
     * Starts resulting packages index file.
     * @param writer Document writer
     * @param cnt Packages count
     * @param type Package type
     * @throws XMLStreamException On error
     */
    static void startDocument(final XMLEventWriter writer, final String cnt, final XmlPackage type)
        throws XMLStreamException {
        writer.add(
            RpmMetadata.EVENTS_FACTORY.createStartDocument(
                StandardCharsets.UTF_8.displayName(), "1.0"
            )
        );
        writer.add(RpmMetadata.EVENTS_FACTORY.createStartElement("", "", type.tag()));
        for (final Map.Entry<String, String> item : type.xmlNamespaces().entrySet()) {
            writer.add(RpmMetadata.EVENTS_FACTORY.createNamespace(item.getKey(), item.getValue()));
        }
        writer.add(RpmMetadata.EVENTS_FACTORY.createAttribute("packages", cnt));
        writer.add(RpmMetadata.EVENTS_FACTORY.createSpace("\n"));
    }

    /**
     * Process lines. Header and root tag opening are written by method
     * {@link MergedXmlPackage#startDocument(XMLEventWriter, String, XmlPackage)} call in
     * {@link MergedXmlPackage#merge(Collection, XmlEvent)}, that's why
     * we skip first two events here.
     * @param ids Not valid ids list
     * @param reader Reader
     * @param writer Writes
     * @throws XMLStreamException When error occurs
     * @checkstyle ParameterNumberCheck (5 lines)
     * @checkstyle CyclomaticComplexityCheck (20 lines)
     */
    private void process(final Collection<String> ids, final XMLEventReader reader,
        final XMLEventWriter writer) throws XMLStreamException {
        boolean valid = true;
        XMLEvent event;
        reader.nextEvent();
        reader.nextEvent();
        while (reader.hasNext()) {
            event = reader.nextEvent();
            if (MergedXmlPackage.isEndTag(event, this.type.tag())) {
                break;
            } else {
                if (event.isStartElement()
                    && event.asStartElement().getName().getLocalPart()
                    .equals(XmlMaid.ByPkgidAttr.TAG)
                ) {
                    valid = !ids.contains(
                        event.asStartElement().getAttributeByName(new QName("pkgid")).getValue()
                    );
                }
                if (valid) {
                    writer.add(event);
                }
                if (MergedXmlPackage.isEndTag(event, XmlMaid.ByPkgidAttr.TAG)) {
                    valid = true;
                }
            }
        }
    }

    /**
     * Is event end tag?
     * @param event Event
     * @param tag Tag name
     * @return True if event is end tag
     */
    private static boolean isEndTag(final XMLEvent event, final String tag) {
        return event.isEndElement()
            && event.asEndElement().getName().getLocalPart().equals(tag);
    }
}

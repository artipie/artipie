/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.misc.UncheckedIOConsumer;
import com.artipie.asto.misc.UncheckedIOScalar;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * Extracts packages names and checksums from primary xml.
 * @since 0.8
 */
public final class XmlPrimaryChecksums {

    /**
     * File path.
     */
    private final InputStream inp;

    /**
     * Ctor.
     * @param path Primary file path
     */
    public XmlPrimaryChecksums(final Path path) {
        this(new UncheckedIOScalar<>(() -> Files.newInputStream(path)).value());
    }

    /**
     * Ctor.
     * @param inp Primary input stream
     */
    public XmlPrimaryChecksums(final InputStream inp) {
        this.inp = inp;
    }

    /**
     * Reads xml.
     * @return Map of packages names and checksums.
     */
    public Map<String, String> read() {
        final Map<String, String> res = new HashMap<>();
        try {
            final XMLEventReader reader = new InputFactoryImpl().createXMLEventReader(this.inp);
            XMLEvent event;
            String name = "";
            String checksum = "";
            while (reader.hasNext()) {
                event = reader.nextEvent();
                if (XmlPrimaryChecksums.isTag(event, "location")) {
                    name = event.asStartElement().getAttributeByName(new QName("href")).getValue();
                }
                if (XmlPrimaryChecksums.isTag(event, "checksum")) {
                    event = reader.nextEvent();
                    checksum = event.asCharacters().getData();
                }
                if (event.isEndElement()
                    && "package".equals(event.asEndElement().getName().getLocalPart())) {
                    res.put(name, checksum);
                }
            }
            reader.close();
        } catch (final XMLStreamException err) {
            throw new ArtipieIOException(err);
        } finally {
            Optional.of(this.inp).ifPresent(new UncheckedIOConsumer<>(InputStream::close));
        }
        return res;
    }

    /**
     * Checks event.
     * @param event Event
     * @param tag Xml tag name
     * @return True is this event is xml tag with given tag name
     */
    private static boolean isTag(final XMLEvent event, final String tag) {
        return event.isStartElement()
            && event.asStartElement().getName().getLocalPart().equals(tag);
    }
}
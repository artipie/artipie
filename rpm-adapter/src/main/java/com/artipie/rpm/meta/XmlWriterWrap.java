/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.meta;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Envelop for XmlFile Class.
 * @since 0.7
 */
@SuppressWarnings("PMD.TooManyMethods")
public class XmlWriterWrap implements XMLStreamWriter {
    /**
     * XML stream.
     */
    private final XMLStreamWriter xml;

    /**
     * Ctor.
     * @param xml XMLStreamWriter object.
     */
    public XmlWriterWrap(final XMLStreamWriter xml) {
        this.xml = xml;
    }

    @Override
    public void writeStartElement(
        final String localname
    ) throws XMLStreamException {
        this.xml.writeStartElement(localname);
    }

    @Override
    public void writeAttribute(
        final String localname,
        final String value
    ) throws XMLStreamException {
        this.xml.writeAttribute(localname, value);
    }

    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    @Override
    public void writeAttribute(
        final String prefix,
        final String namespaceuri,
        final String localname,
        final String value
    ) throws XMLStreamException {
        this.xml.writeAttribute(prefix, namespaceuri, localname, value);
    }

    @Override
    public void writeAttribute(
        final String namespaceuri,
        final String localname,
        final String value
    ) throws XMLStreamException {
        this.xml.writeAttribute(namespaceuri, localname, value);
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        this.xml.writeEndElement();
    }

    @Override
    public void writeStartDocument(
        final String encoding,
        final String version
    ) throws XMLStreamException {
        this.xml.writeStartDocument(encoding, version);
    }

    @Override
    public void writeDefaultNamespace(
        final String namespaceuri
    ) throws XMLStreamException {
        this.xml.writeDefaultNamespace(namespaceuri);
    }

    @Override
    public void writeComment(final String data) throws XMLStreamException {
        this.xml.writeComment(data);
    }

    @Override
    public void writeProcessingInstruction(
        final String target
    ) throws XMLStreamException {
        this.xml.writeProcessingInstruction(target);
    }

    @Override
    public void writeProcessingInstruction(
        final String target,
        final String data
    ) throws XMLStreamException {
        this.xml.writeProcessingInstruction(target, data);
    }

    @Override
    public void writeCData(final String data) throws XMLStreamException {
        this.xml.writeCData(data);
    }

    @Override
    public void writeDTD(final String dtd) throws XMLStreamException {
        this.xml.writeDTD(dtd);
    }

    @Override
    public void writeEntityRef(final String name) throws XMLStreamException {
        this.xml.writeEntityRef(name);
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
        this.xml.writeStartDocument();
    }

    @Override
    public void writeStartDocument(final String version) throws XMLStreamException {
        this.xml.writeStartDocument(version);
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        this.xml.writeEndDocument();
    }

    @Override
    public void close() throws XMLStreamException {
        this.xml.close();
    }

    @Override
    public void flush() throws XMLStreamException {
        this.xml.flush();
    }

    @Override
    public void writeEmptyElement(final String localname) throws XMLStreamException {
        this.xml.writeEmptyElement(localname);
    }

    @Override
    public void writeCharacters(final String text) throws XMLStreamException {
        this.xml.writeCharacters(text);
    }

    @Override
    public void writeCharacters(
        final char[] text,
        final int start,
        final int len
    ) throws XMLStreamException {
        this.xml.writeCharacters(text, start, len);
    }

    @Override
    public String getPrefix(final String uri) throws XMLStreamException {
        return this.xml.getPrefix(uri);
    }

    @Override
    public void setPrefix(final String prefix, final String uri) throws XMLStreamException {
        this.xml.setPrefix(prefix, uri);
    }

    @Override
    public void setDefaultNamespace(final String uri) throws XMLStreamException {
        this.xml.setDefaultNamespace(uri);
    }

    @Override
    public void setNamespaceContext(
        final NamespaceContext context
    ) throws XMLStreamException {
        this.xml.setNamespaceContext(context);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return this.xml.getNamespaceContext();
    }

    @Override
    public Object getProperty(final String name) {
        return this.xml.getProperty(name);
    }

    @Override
    public void writeNamespace(
        final String prefix,
        final String namespaceuri
    ) throws XMLStreamException {
        this.xml.writeNamespace(prefix, namespaceuri);
    }

    @Override
    public void writeEmptyElement(
        final String namespaceuri,
        final String localname
    ) throws XMLStreamException {
        this.xml.writeEmptyElement(namespaceuri, localname);
    }

    @Override
    public void writeEmptyElement(
        final String prefix,
        final String localname,
        final String namespaceuri
    ) throws XMLStreamException {
        this.xml.writeEmptyElement(prefix, localname, namespaceuri);
    }

    @Override
    public void writeStartElement(
        final String namespaceuri,
        final String localname
    ) throws XMLStreamException {
        this.xml.writeStartElement(namespaceuri, localname);
    }

    @Override
    public void writeStartElement(
        final String prefix,
        final String localname,
        final String namespaceuri
    ) throws XMLStreamException {
        this.xml.writeStartElement(prefix, localname, namespaceuri);
    }
}

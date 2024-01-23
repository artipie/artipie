/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.metadata;

import com.artipie.ArtipieException;
import com.artipie.asto.ArtipieIOException;
import com.jcabi.xml.XML;
import com.jcabi.xml.XMLDocument;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Package description in .nuspec format.
 * @since 0.6
 */
@SuppressWarnings("PMD.TooManyMethods")
public interface Nuspec {

    /**
     * Package identifier: original case sensitive and lowercase.
     * @return Package id
     * @throws ArtipieException If id field is not found
         */
    @SuppressWarnings("PMD.ShortMethodName")
    NuspecField id();

    /**
     * Package versions: original version field value or normalised.
     * @return Version of the package
     * @throws ArtipieException If version field is not found
     */
    NuspecField version();

    /**
     * Package description.
     * @return Description
     * @throws ArtipieException If description field is not found
     */
    String description();

    /**
     * Package authors.
     * @return Authors
     * @throws ArtipieException If authors field is not found
     */
    String authors();

    /**
     * Returns `minClientVersion` attribute value. Check
     * <a href="https://learn.microsoft.com/en-us/nuget/reference/nuspec#minclientversion">docs</a>.
     * @return Optional with value if present
     */
    Optional<String> minClientVersion();

    /**
     * Returns optional field by name.
     * @param name Field name
     * @return Optional with value if found
     */
    Optional<String> fieldByName(OptFieldName name);

    /**
     * List of the dependencies formatted as
     * <code>dependency_id:dependency_version:group_targetFramework</code>
     * For more details about format please check
     * <a href="https://docs.microsoft.com/en-us/nuget/reference/nuspec#dependency-groups">docs</a>.
     * @return Dependencies list
     */
    Collection<String> dependencies();

    /**
     * List of the package types formatted as
     * <code>type_name:version</code>
     * For more details about format please check
     * <a href="https://docs.microsoft.com/en-us/nuget/reference/nuspec#packagetypes">docs</a>.
     * @return Dependencies list
     */
    Set<String> packageTypes();

    /**
     * Nuspec file bytes.
     * @return Bytes
     * @throws ArtipieIOException On OI error
     */
    byte[] bytes();

    /**
     * Implementation of {@link Nuspec}, reads fields values from byte xml source.
     *
     * @since 0.6
     */
    final class Xml implements Nuspec {

        /**
         * Xml tag name `version`.
         */
        private static final String VRSN = "version";

        /**
         * Xml document.
         */
        private final XMLDocument content;

        /**
         * Binary content in .nuspec format.
         */
        private final byte[] bytes;

        /**
         * Ctor.
         *
         * @param bytes Binary content of in .nuspec format.
         */
        public Xml(final byte[] bytes) {
            this.bytes = bytes.clone();
            this.content = new XMLDocument(bytes);
        }

        /**
         * Ctor.
         * @param input Input stream with nuspec content
         * @throws ArtipieIOException On IO error
         */
        public Xml(final InputStream input) {
            this(Xml.read(input));
        }

        @Override
        @SuppressWarnings("PMD.ShortMethodName")
        public NuspecField id() {
            return new PackageId(
                single(
                    this.content, "/*[name()='package']/*[name()='metadata']/*[name()='id']/text()"
                )
            );
        }

        @Override
        public NuspecField version() {
            final String version = single(
                this.content,
                "/*[name()='package']/*[name()='metadata']/*[name()='version']/text()"
            );
            return new Version(version);
        }

        @Override
        public String description() {
            return single(
                this.content,
                "/*[name()='package']/*[name()='metadata']/*[name()='description']/text()"
            );
        }

        @Override
        public String authors() {
            return single(
                this.content,
                "/*[name()='package']/*[name()='metadata']/*[name()='authors']/text()"
            );
        }

        @Override
        public Optional<String> minClientVersion() {
            return this.content.xpath("/*[name()='package']/*[name()='metadata']/@minClientVersion")
                .stream().findFirst();
        }

        @Override
        public Optional<String> fieldByName(final OptFieldName name) {
            final List<String> values = this.content.xpath(
                String.format(
                    "/*[name()='package']/*[name()='metadata']/*[name()='%s']/text()", name.get()
                )
            );
            Optional<String> res = Optional.empty();
            if (!values.isEmpty()) {
                res = Optional.of(values.get(0));
            }
            return res;
        }

        @Override
        public Collection<String> dependencies() {
            final List<XML> deps = this.content.nodes(
                "/*[name()='package']/*[name()='metadata']/*[name()='dependencies']"
            );
            final Collection<String> res = new ArrayList<>(10);
            if (!deps.isEmpty()) {
                //@checkstyle LineLengthCheck (1 line)
                final List<XML> groups = this.content.nodes("/*[name()='package']/*[name()='metadata']/*[name()='dependencies']/*[name()='group']");
                for (final XML group : groups) {
                    final String tfv = Optional.ofNullable(
                        group.node().getAttributes().getNamedItem("targetFramework")
                    ).map(Node::getNodeValue).orElse("");
                    final NodeList list = group.node().getChildNodes();
                    boolean empty = true;
                    for (int cnt = 0; cnt < list.getLength(); cnt = cnt + 1) {
                        final Node item = list.item(cnt);
                        if ("dependency".equals(item.getLocalName())) {
                            empty = false;
                            final String id = Optional.ofNullable(
                                item.getAttributes().getNamedItem("id")
                            ).map(Node::getNodeValue).orElse("");
                            final String version = Optional.ofNullable(
                                item.getAttributes().getNamedItem(Xml.VRSN)
                            ).map(Node::getNodeValue).orElse("");
                            res.add(String.format("%s:%s:%s", id, version, tfv));
                        }
                    }
                    if (empty) {
                        res.add(String.format("::%s", tfv));
                    }
                }
            }
            return res;
        }

        @Override
        public Set<String> packageTypes() {
            final List<XML> root = this.content.nodes(
                "/*[name()='package']/*[name()='metadata']/*[name()='packageTypes']"
            );
            final Set<String> res = new HashSet<>(1);
            if (!root.isEmpty()) {
                //@checkstyle LineLengthCheck (1 line)
                final List<XML> types = this.content.nodes("/*[name()='package']/*[name()='metadata']/*[name()='packageTypes']/*[name()='packageType']");
                for (final XML type : types) {
                    res.add(
                        String.format(
                            "%s:%s",
                            type.node().getAttributes().getNamedItem("name").getNodeValue(),
                            Optional.ofNullable(
                                type.node().getAttributes().getNamedItem(Xml.VRSN)
                            ).map(Node::getNodeValue).orElse("")
                        )
                    );
                }
            }
            return res;
        }

        @Override
        public byte[] bytes() {
            return this.bytes.clone();
        }

        @Override
        public String toString() {
            return new String(this.bytes(), StandardCharsets.UTF_8);
        }

        /**
         * Reads single string value from XML via XPath.
         * Exception is thrown if zero or more then 1 values found
         *
         * @param xml XML document to read from.
         * @param xpath XPath expression to select data from the XML.
         * @return Value found by XPath
         */
        private static String single(final XML xml, final String xpath) {
            final List<String> values = xml.xpath(xpath);
            if (values.isEmpty()) {
                throw new ArtipieException(
                    new IllegalArgumentException(
                        String.format("No values found in path: '%s'", xpath)
                    )
                );
            }
            if (values.size() > 1) {
                throw new ArtipieException(
                    new IllegalArgumentException(
                        String.format("Multiple values found in path: '%s'", xpath)
                    )
                );
            }
            return values.get(0);
        }

        /**
         * Read bytes from input stream.
         * @param input Input to read from
         * @return Bytes
         */
        private static byte[] read(final InputStream input) {
            try {
                return IOUtils.toByteArray(input);
            } catch (final IOException err) {
                throw new ArtipieIOException(err);
            }
        }
    }
}

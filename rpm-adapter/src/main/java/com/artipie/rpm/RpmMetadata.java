/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm;

import com.artipie.asto.ArtipieIOException;
import com.artipie.rpm.meta.MergedXml;
import com.artipie.rpm.meta.MergedXmlPackage;
import com.artipie.rpm.meta.MergedXmlPrimary;
import com.artipie.rpm.meta.XmlAlter;
import com.artipie.rpm.meta.XmlEvent;
import com.artipie.rpm.meta.XmlEventPrimary;
import com.artipie.rpm.meta.XmlMaid;
import com.artipie.rpm.meta.XmlPackage;
import com.artipie.rpm.meta.XmlPrimaryMaid;
import com.artipie.rpm.pkg.Checksum;
import com.artipie.rpm.pkg.FilePackage;
import com.artipie.rpm.pkg.Package;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.stax.InputFactoryImpl;
import com.fasterxml.aalto.stax.OutputFactoryImpl;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.xml.stream.XMLEventFactory;
import org.codehaus.stax2.XMLOutputFactory2;
import org.redline_rpm.header.AbstractHeader;
import org.redline_rpm.header.Header;

/**
 * Rpm metadata class works with xml metadata - adds or removes records about xml packages.
 * @since 1.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ModifierOrderCheck (500 lines)
 * @checkstyle InterfaceIsTypeCheck (500 lines)
 * @checkstyle NestedTryDepthCheck (500 lines)
 */
public interface RpmMetadata {

    /**
     * Temp file suffix.
     */
    String SUFFIX = ".xml";

    /**
     * Xml Events Factory.
     */
    XMLEventFactory EVENTS_FACTORY = XMLEventFactory.newFactory();
    /**
     * Xml Output Factory.
     */
    XMLOutputFactory2 OUTPUT_FACTORY = new OutputFactoryImpl();
    /**
     * Xml Input Factory.
     */
    AsyncXMLInputFactory INPUT_FACTORY = new InputFactoryImpl();

    /**
     * Removes RMP records from metadata.
     *
     * @since 1.4
     */
    final class Remove {

        /**
         * Metadata list.
         */
        private final Collection<MetadataItem> items;

        /**
         * Ctor.
         * @param items Metadata items
         */
        public Remove(final MetadataItem... items) {
            this.items = Arrays.asList(items);
        }

        /**
         * Removes records from metadata by RPMs checksums.
         * @param checksums Rpms checksums  to remove by
         * @throws ArtipieIOException On io-operation result error
         * @checkstyle NestedTryDepthCheck (30 lines)
         */
        public void perform(final Collection<String> checksums) {
            try {
                for (final MetadataItem item : this.items) {
                    if (!item.input.isPresent()) {
                        continue;
                    }
                    final Path temp = Files.createTempFile("rpm-index", RpmMetadata.SUFFIX);
                    try {
                        final long res;
                        final XmlMaid maid;
                        try (OutputStream out =
                            new BufferedOutputStream(Files.newOutputStream(temp))) {
                            if (item.type == XmlPackage.PRIMARY) {
                                maid = new XmlPrimaryMaid.Stream(item.input.get(), out);
                            } else {
                                maid = new XmlMaid.ByPkgidAttr.Stream(item.input.get(), out);
                            }
                            res = maid.clean(checksums);
                        }
                        try (InputStream input =
                            new BufferedInputStream(Files.newInputStream(temp))) {
                            new XmlAlter.Stream(input, item.out)
                                .pkgAttr(item.type.tag(), String.valueOf(res));
                        }
                    } finally {
                        Files.delete(temp);
                    }
                }
            } catch (final IOException err) {
                throw new ArtipieIOException(err);
            }
        }
    }

    /**
     * Appends RMP records into metadata.
     *
     * @since 1.4
     */
    final class Append {
        /**
         * Xml Event Primary.
         */
        private final static XmlEventPrimary XML_EVENT_PRIMARY = new XmlEventPrimary();

        /**
         * Metadata list.
         */
        private final Collection<MetadataItem> items;

        /**
         * Ctor.
         * @param items Metadata items
         */
        public Append(final MetadataItem... items) {
            this.items = Arrays.asList(items);
        }

        /**
         * Appends records about provided RPMs.
         * @param packages Rpms to append info about, map of the path to file and location
         * @throws ArtipieIOException On io-operation error
         * @checkstyle NestedTryDepthCheck (20 lines)
         */
        @SuppressWarnings("PMD.AvoidDuplicateLiterals")
        public void perform(final Collection<Package.Meta> packages) {
            try {
                final Path temp = Files.createTempFile("rpm-primary-append", RpmMetadata.SUFFIX);
                try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(temp))) {
                    final MetadataItem primary = this.items.stream()
                        .filter(item -> item.type == XmlPackage.PRIMARY)
                        .findFirst()
                        .orElseThrow(
                            () -> new ArtipieIOException(
                                String.format("Should have a file with type %s", XmlPackage.PRIMARY)
                            )
                        );
                    final MergedXml.Result res = new MergedXmlPrimary(primary.input, out)
                        .merge(packages, Append.XML_EVENT_PRIMARY);
                    final CompletableFuture<Void> fut = CompletableFuture.allOf(
                        CompletableFuture.runAsync(this.updateOther(packages, res)),
                        CompletableFuture.runAsync(this.updateFilelist(packages, res))
                    );
                    try (InputStream input = new BufferedInputStream(Files.newInputStream(temp))) {
                        new XmlAlter.Stream(input, primary.out)
                            .pkgAttr(primary.type.tag(), String.valueOf(res.count()));
                    }
                    fut.join();
                    Files.delete(temp);
                }
            } catch (final IOException err) {
                throw new ArtipieIOException(err);
            }
        }

        /**
         * Creates runnable action to update filelist.xml index.
         * @param packages Packages to add
         * @param res Xml update primary result
         * @return Action
         */
        private Runnable updateFilelist(final Collection<Package.Meta> packages,
            final MergedXml.Result res) {
            return () -> {
                final Optional<MetadataItem> filelist = this.items.stream()
                    .filter(item -> item.type == XmlPackage.FILELISTS).findFirst();
                if (filelist.isPresent()) {
                    try {
                        new MergedXmlPackage(
                            filelist.get().input, filelist.get().out, XmlPackage.FILELISTS, res
                        ).merge(packages, new XmlEvent.Filelists());
                    } catch (final IOException err) {
                        throw new ArtipieIOException(err);
                    }
                }
            };
        }

        /**
         * Creates runnable action to update other.xml index.
         * @param packages Packages to add
         * @param res Xml update primary result
         * @return Action
         */
        private Runnable updateOther(final Collection<Package.Meta> packages,
            final MergedXml.Result res) {
            return () -> {
                try {
                    final MetadataItem other = this.items.stream()
                        .filter(item -> item.type == XmlPackage.OTHER)
                        .findFirst().orElseThrow(
                            () -> new ArtipieIOException(
                                String.format("Should have a file with type %s", XmlPackage.OTHER)
                            )
                        );
                    new MergedXmlPackage(other.input, other.out, XmlPackage.OTHER, res)
                        .merge(packages, new XmlEvent.Other());
                } catch (final IOException err) {
                    throw new ArtipieIOException(err);
                }
            };
        }
    }

    /**
     * Metadata item.
     * @since 1.4
     */
    final class MetadataItem {

        /**
         * Xml metadata type.
         */
        private final XmlPackage type;

        /**
         * Xml metadata input stream.
         */
        private final Optional<InputStream> input;

        /**
         * Xml metadata output, where write the result.
         */
        private final OutputStream out;

        /**
         * Ctor.
         * @param type Xml type
         * @param input Xml metadata input stream
         * @param out Xml metadata output, where write the result
         */
        public MetadataItem(final XmlPackage type, final Optional<InputStream> input,
            final OutputStream out) {
            this.type = type;
            this.input = input;
            this.out = out;
        }

        /**
         * Ctor.
         * @param type Xml type
         * @param input Xml metadata input stream
         * @param out Xml metadata output, where write the result
         */
        public MetadataItem(final XmlPackage type, final InputStream input,
            final OutputStream out) {
            this(type, Optional.of(input), out);
        }

        /**
         * Ctor.
         * @param type Xml type
         * @param out Xml metadata output, where write the result
         */
        public MetadataItem(final XmlPackage type, final OutputStream out) {
            this(type, Optional.empty(), out);
        }
    }

    /**
     * Rpm file item.
     * @since 1.8
     */
    final class RpmItem implements Package.Meta {

        /**
         * Rpm file header.
         */
        private final Header header;

        /**
         * File size.
         */
        private final long size;

        /**
         * File checksum and algorithm.
         */
        private final Checksum sum;

        /**
         * Relative file location in the repository, value of
         * location tag from primary xml.
         */
        private final String location;

        /**
         * Ctor.
         * @param header Rpm file header
         * @param size File size
         * @param sum File checksum and algorithm
         * @param location Relative file location in the repository, value of
         *  location tag from primary xml
         * @checkstyle ParameterNumberCheck (5 lines)
         */
        public RpmItem(final Header header, final long size, final Checksum sum,
            final String location) {
            this.header = header;
            this.size = size;
            this.sum = sum;
            this.location = location;
        }

        /**
         * Ctor with SHA256 as default checksum algorithm.
         * @param header Rpm file header
         * @param size File size
         * @param dgst File checksum
         * @param location Relative file location in the repository, value of
         *  location tag from primary xml
         * @checkstyle ParameterNumberCheck (5 lines)
         */
        public RpmItem(final Header header, final long size, final String dgst,
            final String location) {
            this(header, size, new Checksum.Simple(Digest.SHA256, dgst), location);
        }

        @Override
        public Package.MetaHeader header(final AbstractHeader.Tag tag) {
            return new FilePackage.EntryHeader(this.header.getEntry(tag));
        }

        @Override
        public Checksum checksum() {
            return this.sum;
        }

        @Override
        public long size() {
            return this.size;
        }

        @Override
        public String href() {
            return this.location;
        }

        @Override
        public int[] range() {
            return new int[] {
                this.header.getStartPos(),
                this.header.getEndPos(),
            };
        }
    }
}

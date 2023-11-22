/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi.meta;

import com.artipie.asto.ArtipieIOException;
import com.jcabi.log.Logger;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.z.ZCompressorInputStream;
import org.apache.commons.io.IOUtils;

/**
 * Python package metadata.
 * @since 0.6
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public interface Metadata {

    /**
     * Read package metadata from python artifact.
     * @return Instance of {@link PackageInfo}.
     */
    PackageInfo read();

    /**
     * Metadata from archive implementation.
     * @since 0.6
     */
    final class FromArchive implements Metadata {

        /**
         * Archive input stream.
         */
        private final InputStream input;

        /**
         * Name of the file.
         */
        private final String filename;

        /**
         * Ctor.
         * @param input Path to archive
         * @param filename Filename
         */
        public FromArchive(final InputStream input, final String filename) {
            this.input = input;
            this.filename = filename;
        }

        @Override
        public PackageInfo read() {
            final PackageInfo res;
            if (Stream.of("zip", "whl", "egg").anyMatch(this.filename::endsWith)) {
                res = this.readZipEggOrWhl();
            } else if (this.filename.endsWith("tar")) {
                res = this.readTar();
            } else if (this.filename.endsWith("tar.gz")) {
                res = this.readTarGz();
            } else if (this.filename.endsWith("tar.Z")) {
                res = this.readTarZ();
            } else if (this.filename.endsWith("tar.bz2")) {
                res = this.readBz();
            } else {
                throw new UnsupportedOperationException("Unsupported archive type");
            }
            return res;
        }

        /**
         * Reads tar.Z files.
         * @return PackageInfo
         */
        private PackageInfo readTarZ() {
            try (
                ZCompressorInputStream origin = new ZCompressorInputStream(
                    new BufferedInputStream(this.input)
                )
            ) {
                return FromArchive.unpack(origin);
            } catch (final IOException | ArchiveException ex) {
                throw FromArchive.error(ex);
            }
        }

        /**
         * Reads tar.Z files.
         * @return PackageInfo
         */
        private PackageInfo readBz() {
            try (
                BZip2CompressorInputStream origin = new BZip2CompressorInputStream(
                    new BufferedInputStream(this.input)
                )
            ) {
                return FromArchive.unpack(origin);
            } catch (final IOException | ArchiveException ex) {
                throw FromArchive.error(ex);
            }
        }

        /**
         * Reads metadata from zip, egg or wheel archive.
         * @return PackageInfo
         */
        private PackageInfo readZipEggOrWhl() {
            try (ZipArchiveInputStream archive =
                new ZipArchiveInputStream(new BufferedInputStream(this.input))
            ) {
                return FromArchive.readArchive(archive);
            } catch (final IOException ex) {
                Logger.error(this, ex.getMessage());
                throw FromArchive.error(ex);
            }
        }

        /**
         * Reads metadata from zip, egg or wheel archive.
         * @return PackageInfo
         */
        private PackageInfo readTar() {
            try (ArchiveInputStream archive =
                new TarArchiveInputStream(new BufferedInputStream(this.input))
            ) {
                return FromArchive.readArchive(archive);
            } catch (final IOException ex) {
                throw FromArchive.error(ex);
            }
        }

        /**
         * Reads metadata from zip or tar archive.
         * @return PackageInfo
         */
        private PackageInfo readTarGz() {
            try (GzipCompressorInputStream archive = new GzipCompressorInputStream(this.input);
                TarArchiveInputStream tar = new TarArchiveInputStream(archive)) {
                return FromArchive.readArchive(tar);
            } catch (final IOException ex) {
                throw FromArchive.error(ex);
            }
        }

        /**
         * Reads archive from compressor input stream, creates ArchiveInputStream and
         * calls {@link FromArchive#readArchive} to extract metadata info.
         * @param origin Origin input stream
         * @return Package info
         * @throws IOException On IO error
         * @throws ArchiveException In case on problems to unpack
         */
        private static PackageInfo unpack(final InputStream origin)
            throws IOException, ArchiveException {
            try (
                ArchiveInputStream archive = new ArchiveStreamFactory().createArchiveInputStream(
                    new BufferedInputStream(origin)
                )
            ) {
                return readArchive(archive);
            }
        }

        /**
         * Error.
         * @param err Original exception
         * @return IllegalArgumentException instance
         */
        private static ArtipieIOException error(final Exception err) {
            return new ArtipieIOException("Failed to parse python package", err);
        }

        /**
         * Reads archive.
         * @param input Archive to read
         * @return PackageInfo if package info file found
         * @throws IOException On error
         */
        @SuppressWarnings("PMD.AssignmentInOperand")
        private static PackageInfo readArchive(final ArchiveInputStream input) throws IOException {
            ArchiveEntry entry;
            Optional<PackageInfo> res = Optional.empty();
            while ((entry = input.getNextEntry()) != null) {
                if (!input.canReadEntryData(entry) || entry.isDirectory()) {
                    continue;
                }
                if (entry.getName().contains("PKG-INFO") || entry.getName().contains("METADATA")) {
                    res = Optional.of(
                        new PackageInfo.FromMetadata(
                            IOUtils.toString(input, StandardCharsets.US_ASCII)
                        )
                    );
                }
            }
            return res.orElseThrow(
                () -> new ArtipieIOException("Package metadata file not found")
            );
        }

    }

}

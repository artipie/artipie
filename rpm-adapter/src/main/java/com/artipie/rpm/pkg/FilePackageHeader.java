/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.asto.misc.UncheckedIOScalar;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import org.redline_rpm.ReadableChannelWrapper;
import org.redline_rpm.Scanner;
import org.redline_rpm.header.Format;
import org.redline_rpm.header.Header;

/**
 * Header of RPM package file.
 *
 * @since 0.10
 */
public final class FilePackageHeader {

    /**
     * The RPM file input stream.
     */
    private final InputStream pckg;

    /**
     * Ctor.
     *
     * @param file The RPM file.
     */
    public FilePackageHeader(final InputStream file) {
        this.pckg = file;
    }

    /**
     * Ctor.
     *
     * @param file The RPM file.
     */
    public FilePackageHeader(final Path file) {
        this(new UncheckedIOScalar<>(() -> Files.newInputStream(file)).value());
    }

    /**
     * Get header.
     * Note: after the header was read from channel, for proper work of piped IO streams in
     * {@link com.artipie.asto.streams.ContentAsStream}, it's necessary fully read the channel.
     * @return The header.
     * @throws InvalidPackageException In case package is invalid.
     * @throws IOException In case of I/O error.
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public Header header() throws InvalidPackageException, IOException {
        try (ReadableByteChannel chan = Channels.newChannel(this.pckg)) {
            final Format format;
            try {
                format = new Scanner(
                    new PrintStream(Logger.stream(Level.FINE, this))
                ).run(new ReadableChannelWrapper(chan));
                // @checkstyle IllegalCatchCheck (1 line)
            } catch (final RuntimeException ex) {
                throw new InvalidPackageException(ex);
            }
            final Header header = format.getHeader();
            Logger.debug(this, "header: %s", header.toString());
            final int bufsize = 1024;
            int read = 1;
            while (read > 0) {
                read = chan.read(ByteBuffer.allocate(bufsize));
            }
            return header;
        }
    }
}

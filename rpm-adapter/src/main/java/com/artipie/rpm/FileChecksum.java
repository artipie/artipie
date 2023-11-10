/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm;

import com.artipie.rpm.pkg.Checksum;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Locale;
import javax.xml.bind.DatatypeConverter;

/**
 * Hashing sum of a file.
 *
 * @since 0.1
 */
public final class FileChecksum implements Checksum {

    /**
     * Default file buffer is 8K.
     */
    private static final int BUF_SIZE = 1024 * 8;

    /**
     * The XML.
     */
    private final Path file;

    /**
     * Message digest.
     */
    private final Digest dgst;

    /**
     * Ctor.
     * @param path The path
     * @param dgst The hashing algorithm for checksum computation
     */
    public FileChecksum(final Path path, final Digest dgst) {
        this.file = path;
        this.dgst = dgst;
    }

    @Override
    public Digest digest() {
        return this.dgst;
    }

    @Override
    public String hex() throws IOException {
        final MessageDigest digest = this.dgst.messageDigest();
        try (FileChannel chan = FileChannel.open(this.file, StandardOpenOption.READ)) {
            final ByteBuffer buf = ByteBuffer.allocateDirect(FileChecksum.BUF_SIZE);
            while (chan.read(buf) > 0) {
                ((Buffer) buf).flip();
                digest.update(buf);
                buf.clear();
            }
        }
        return DatatypeConverter.printHexBinary(digest.digest())
            .toLowerCase(Locale.US);
    }
}

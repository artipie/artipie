/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.debian.misc;

import com.artipie.ArtipieException;
import com.artipie.asto.ArtipieIOException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Function;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Calculates size and digest of the gz packed content provided as input stream.
 * @since 0.6
 */
@SuppressWarnings("PMD.AssignmentInOperand")
public final class SizeAndDigest implements Function<InputStream, Pair<Long, String>> {

    @Override
    public Pair<Long, String> apply(final InputStream input) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long size = 0;
            try (GzipCompressorInputStream gcis = new GzipCompressorInputStream(input)) {
                // @checkstyle MagicNumberCheck (1 line)
                final byte[] buf = new byte[1024];
                int cnt;
                while (-1 != (cnt = gcis.read(buf))) {
                    digest.update(buf, 0, cnt);
                    size = size + cnt;
                }
                return new ImmutablePair<>(size, Hex.encodeHexString(digest.digest()));
            }
        } catch (final NoSuchAlgorithmException err) {
            throw new ArtipieException(err);
        } catch (final IOException err) {
            throw new ArtipieIOException(err);
        }
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.debian.misc;

import com.artipie.ArtipieException;
import com.artipie.asto.ArtipieIOException;
import com.jcabi.log.Logger;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.util.Iterator;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

/**
 * Gpg signature, ain functionality of this class was copy-pasted from
 * https://github.com/bcgit/bc-java/blob/master/pg/src/main/java/org/bouncycastle/openpgp/examples/ClearSignedFileProcessor.java.
 * @since 0.4
 * @checkstyle ExecutableStatementCountCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle InnerAssignmentCheck (500 lines)
 */
@SuppressWarnings(
    {"PMD.AvoidDuplicateLiterals", "PMD.AssignmentInOperand", "PMD.ArrayIsStoredDirectly"}
)
public final class GpgClearsign {

    /**
     * Bytes content to sign.
     */
    private final byte[] content;

    /**
     * Ctor.
     * @param content Bytes content to sign
     */
    public GpgClearsign(final byte[] content) {
        this.content = content;
    }

    /**
     * Signs content with GPG clearsign signature and returns it along with the signature.
     * @param key Private key bytes
     * @param pass Password
     * @return File, signed with gpg
     * @throws ArtipieIOException On IO errors
     * @throws ArtipieException On problems with GPG
     */
    public byte[] signedContent(final byte[] key, final String pass) {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final ArmoredOutputStream armored = new ArmoredOutputStream(out);
            try (
                InputStream input = new BufferedInputStream(
                    new ByteArrayInputStream(this.content)
                );
                ByteArrayOutputStream line = new ByteArrayOutputStream()
            ) {
                final PGPSignatureGenerator sgen = GpgClearsign.prepareGenerator(key, pass);
                armored.beginClearText(PGPUtil.SHA256);
                int ahead = readInputLine(line, input);
                GpgClearsign.processLine(armored, sgen, line.toByteArray());
                if (ahead != -1) {
                    do {
                        ahead = GpgClearsign.readInputLine(line, ahead, input);
                        sgen.update((byte) '\r');
                        sgen.update((byte) '\n');
                        GpgClearsign.processLine(armored, sgen, line.toByteArray());
                    }
                    while (ahead != -1);
                }
                armored.endClearText();
                final BCPGOutputStream bout = new BCPGOutputStream(armored);
                sgen.generate().encode(bout);
                armored.close();
                return out.toByteArray();
            }
        } catch (final PGPException err) {
            Logger.error(this, "Error while generating gpg-signature:\n%s", err.getMessage());
            throw new ArtipieException(err);
        } catch (final IOException err) {
            Logger.error(this, "IO error while generating gpg-signature:\n%s", err.getMessage());
            throw new ArtipieIOException(err);
        }
    }

    /**
     * Signs content with GPG clearsign signature and returns the signature.
     * @param key Private key bytes
     * @param pass Password
     * @return File, signed with gpg
     * @throws ArtipieIOException On IO errors
     * @throws ArtipieException On problems with GPG
     */
    public byte[] signature(final byte[] key, final String pass) {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final ArmoredOutputStream armored = new ArmoredOutputStream(out);
            try (
                InputStream input = new BufferedInputStream(
                    new ByteArrayInputStream(this.content)
                )
            ) {
                armored.setHeader(ArmoredOutputStream.VERSION_HDR, null);
                final PGPSignatureGenerator sgen = prepareGenerator(key, pass);
                int sym;
                while ((sym = input.read()) >= 0) {
                    sgen.update((byte) sym);
                }
                final BCPGOutputStream res = new BCPGOutputStream(armored);
                sgen.generate().encode(res);
                armored.close();
                return out.toByteArray();
            }
        } catch (final PGPException err) {
            Logger.error(this, "Error while generating gpg-signature:\n%s", err.getMessage());
            throw new ArtipieException(err);
        } catch (final IOException err) {
            Logger.error(this, "IO error while generating gpg-signature:\n%s", err.getMessage());
            throw new ArtipieIOException(err);
        }
    }

    /**
     * Prepares signature generator.
     * @param key Private key
     * @param pass Password
     * @return Instance of PGPSignatureGenerator
     * @throws IOException On error
     * @throws PGPException On problems with signing
     */
    private static PGPSignatureGenerator prepareGenerator(final byte[] key, final String pass)
        throws IOException, PGPException {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        final PGPSecretKey skey = readSecretKey(new ByteArrayInputStream(key));
        final PGPPrivateKey pkey = skey.extractPrivateKey(
            new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(pass.toCharArray())
        );
        final PGPSignatureGenerator sgen = new PGPSignatureGenerator(
            new JcaPGPContentSignerBuilder(skey.getPublicKey().getAlgorithm(), PGPUtil.SHA256)
                .setProvider("BC")
        );
        final PGPSignatureSubpacketGenerator ssgen = new PGPSignatureSubpacketGenerator();
        sgen.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, pkey);
        final Iterator<String> ids = skey.getPublicKey().getUserIDs();
        if (ids.hasNext()) {
            ssgen.addSignerUserID(false, ids.next());
            sgen.setHashedSubpackets(ssgen.generate());
        }
        return sgen;
    }

    /**
     * Reads secret key from provided input stream.
     * @param input Input stream to read stream from
     * @return Instance of PGPSecretKey
     * @throws IOException On IO errors
     * @throws PGPException On Keys errors
     */
    private static PGPSecretKey readSecretKey(final InputStream input)
        throws IOException, PGPException {
        final Iterator<PGPSecretKeyRing> keys = new PGPSecretKeyRingCollection(
            PGPUtil.getDecoderStream(input), new JcaKeyFingerprintCalculator()
        ).getKeyRings();
        while (keys.hasNext()) {
            final Iterator<PGPSecretKey> skey = keys.next().getSecretKeys();
            while (skey.hasNext()) {
                final PGPSecretKey key = skey.next();
                if (key.isSigningKey()) {
                    return key;
                }
            }
        }
        throw new IllegalArgumentException("Can't find signing key in key ring.");
    }

    /**
     * Process line, trailing white space needs to be removed from the end of each line for
     * signature calculation according to RFC 4880 Section 7.1.
     * @param out Where to write
     * @param sign Signature generator
     * @param line Line to process
     * @throws IOException On error
     */
    private static void processLine(final OutputStream out, final PGPSignatureGenerator sign,
        final byte[] line) throws IOException {
        final int length = getLengthWithoutWhiteSpace(line);
        if (length > 0) {
            sign.update(line, 0, length);
        }
        out.write(line, 0, line.length);
    }

    /**
     * Line length without whitespace.
     * @param line Line to measure
     * @return Length
     */
    private static int getLengthWithoutWhiteSpace(final byte[] line) {
        int end = line.length - 1;
        while (end >= 0 && GpgClearsign.isWhiteSpace(line[end])) {
            end = end - 1;
        }
        return end + 1;
    }

    /**
     * Is symbol a whitespace?
     * @param sym Symbol
     * @return True if it is a whitespace
     */
    private static boolean isWhiteSpace(final byte sym) {
        return GpgClearsign.isLineEnding(sym) || sym == '\t' || sym == ' ';
    }

    /**
     * Is symbol an end of the line?
     * @param sym Symbol
     * @return True if it is
     */
    private static boolean isLineEnding(final byte sym) {
        return sym == '\r' || sym == '\n';
    }

    /**
     * Reads input line.
     * @param out Where to write
     * @param input Where to read from
     * @return Symbols ahead
     * @throws IOException On IO error
     */
    private static int readInputLine(final ByteArrayOutputStream out, final InputStream input)
        throws IOException {
        out.reset();
        int ahead = -1;
        int sym;
        while ((sym = input.read()) >= 0) {
            out.write(sym);
            if (GpgClearsign.isLineEnding((byte) sym)) {
                ahead = GpgClearsign.readPassedEol(out, sym, input);
                break;
            }
        }
        return ahead;
    }

    /**
     * Reads input line.
     * @param out Where to write
     * @param ahead Already read
     * @param input Where to read from
     * @return Symbols ahead
     * @throws IOException On IO error
     */
    private static int readInputLine(final ByteArrayOutputStream out, final int ahead,
        final InputStream input) throws IOException {
        out.reset();
        int cnt = ahead;
        int res = ahead;
        do {
            out.write(cnt);
            if (cnt == '\r' || cnt == '\n') {
                res = GpgClearsign.readPassedEol(out, cnt, input);
                break;
            }
        }
        while ((cnt = input.read()) >= 0);
        if (cnt < 0) {
            res = -1;
        }
        return res;
    }

    /**
     * Reads end of line.
     * @param out Where to write
     * @param last Symbol
     * @param input Where to read from
     * @return Symbols ahead
     * @throws IOException On IO error
     */
    private static int readPassedEol(final ByteArrayOutputStream out, final int last,
        final InputStream input) throws IOException {
        int ahead = input.read();
        if (last == '\r' && ahead == '\n') {
            out.write(ahead);
            ahead = input.read();
        }
        return ahead;
    }
}

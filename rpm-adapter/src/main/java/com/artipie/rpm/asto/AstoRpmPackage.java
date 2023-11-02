/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.ArtipieException;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.misc.UncheckedIOFunc;
import com.artipie.asto.streams.ContentAsStream;
import com.artipie.rpm.Digest;
import com.artipie.rpm.RpmMetadata;
import com.artipie.rpm.pkg.Checksum;
import com.artipie.rpm.pkg.FilePackageHeader;
import com.artipie.rpm.pkg.Package;
import java.util.concurrent.CompletionStage;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.redline_rpm.header.Header;

/**
 * Rpm package metadata from the storage.
 * @since 1.9
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class AstoRpmPackage {

    /**
     * Asto storage.
     */
    private final Storage asto;

    /**
     * Digest algorithm.
     */
    private final Digest dgst;

    /**
     * Ctor.
     * @param asto Asto storage
     * @param dgst Digest algorithm
     */
    public AstoRpmPackage(final Storage asto, final Digest dgst) {
        this.asto = asto;
        this.dgst = dgst;
    }

    /**
     * Obtain rpm package metadata, instance of {@link Package.Meta}.
     * @param key Package key
     * @return Completable action
     */
    public CompletionStage<Package.Meta> packageMeta(final Key key) {
        return this.packageMeta(key, key.string());
    }

    /**
     * Obtain rpm package metadata, instance of {@link Package.Meta}.
     * @param key Package key
     * @param path Package repository relative path
     * @return Completable action
     */
    public CompletionStage<Package.Meta> packageMeta(final Key key, final String path) {
        return this.asto.value(key).thenCompose(
            val -> new ContentDigest(val, this.dgst::messageDigest).hex().thenApply(
                hex -> new ImmutablePair<>(
                    hex,
                    val.size().orElseThrow(() -> new ArtipieException("Content size unknown!"))
                )
            )
        ).thenCompose(
            pair -> this.asto.value(key).thenCompose(
                val -> new ContentAsStream<Header>(val).process(
                    new UncheckedIOFunc<>(input -> new FilePackageHeader(input).header())
                ).thenApply(
                    header -> new RpmMetadata.RpmItem(
                        header, pair.getValue(), new Checksum.Simple(this.dgst, pair.getKey()), path
                    )
                )
            )
        );
    }
}

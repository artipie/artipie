/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.hm;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.rpm.Digest;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.meta.XmlPackage;
import com.jcabi.xml.XMLDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;
import org.llorllale.cactoos.matchers.MatcherOf;

/**
 * Matcher for checking rempomd.xml file presence and information in the storage.
 *
 * @since 1.1
 */
public final class StorageHasRepoMd extends AllOf<Storage> {

    /**
     * Repodata key.
     */
    private static final Key BASE = new Key.From("repodata");

    /**
     * Repomd rey.
     */
    private static final Key.From REPOMD = new Key.From(StorageHasRepoMd.BASE, "repomd.xml");

    /**
     * Ctor.
     * @param config Rmp repo config
     */
    public StorageHasRepoMd(final RepoConfig config) {
        super(matchers(config));
    }

    /**
     * List of matchers.
     * @param config Rmp repo config
     * @return Matchers list
     */
    private static List<Matcher<? super Storage>> matchers(final RepoConfig config) {
        final List<Matcher<? super Storage>> res = new ArrayList<>(4);
        res.add(
            new MatcherOf<>(
                storage -> storage.exists(StorageHasRepoMd.REPOMD).join(),
                desc -> desc.appendText("Repomd is present"),
                (sto, desc) ->  desc.appendText("Repomd is not present")
            )
        );
        new XmlPackage.Stream(config.filelists()).get().forEach(
            pkg -> res.add(
                new MatcherOf<>(
                    storage -> hasRecord(storage, pkg, config.digest()),
                    desc -> desc.appendText(
                        String.format("Repomd has record for %s xml", pkg.name())
                    ),
                    (sto, desc) ->  String.format("Repomd has not record for %s xml", pkg.name())
                )
            )
        );
        return res;
    }

    /**
     * Has repomd record for xml metadata package?
     * @param storage Storage
     * @param pckg Metadata package
     * @param digest Digest algorithm
     * @return True if record is present
     */
    private static boolean hasRecord(final Storage storage, final XmlPackage pckg,
        final Digest digest) {
        final Optional<Content> repomd = storage.list(StorageHasRepoMd.BASE).join().stream()
            .filter(item -> item.string().contains(pckg.lowercase())).findFirst()
            .map(item -> storage.value(new Key.From(item)).join());
        boolean res = false;
        if (repomd.isPresent()) {
            final String checksum = new ContentDigest(
                repomd.get(),
                digest::messageDigest
            ).hex().toCompletableFuture().join();
            res = !new XMLDocument(
                new PublisherAs(storage.value(StorageHasRepoMd.REPOMD).join())
                    .asciiString().toCompletableFuture().join()
            ).nodes(
                String.format(
                    //@checkstyle LineLengthCheck (1 line)
                    "/*[name()='repomd']/*[name()='data' and @type='%s']/*[name()='checksum' and @type='%s' and text()='%s']",
                    pckg.name().toLowerCase(Locale.US),
                    digest.type(),
                    checksum
                )
            ).isEmpty();
        }
        return res;
    }
}

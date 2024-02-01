/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.hm;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.rpm.files.Gzip;
import com.artipie.rpm.meta.XmlPackage;
import com.jcabi.xml.XMLDocument;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;
import org.llorllale.cactoos.matchers.MatcherOf;

/**
 * Storage has metadata matcher checks that metadata files primary.xml, other.xml and
 * filelists.xml (optional) are present in the storage and have information about expected
 * amount of rpm packages (see {@link NodeHasPkgCount}). Storage should contain gzipped metadata
 * files by {@code repodata} key, each metadata file should be met only once.
 * @since 0.11
 */
public final class StorageHasMetadata extends AllOf<Storage> {

    /**
     * Ctor.
     * @param expected Amount of expected items in metadata
     * @param filelists Need filelist
     * @param temp Temp dir to unpack xml
     */
    public StorageHasMetadata(final int expected, final boolean filelists, final Path temp) {
        super(StorageHasMetadata.matchers(expected, filelists, temp));
    }

    /**
     * List of matchers.
     * @param expected Amount of expected items in metadata
     * @param filelists Need filelist
     * @param temp Temp dir to unpack xml
     * @return Matchers list
     */
    private static List<Matcher<? super Storage>> matchers(
        final int expected, final boolean filelists, final Path temp
    ) {
        return new XmlPackage.Stream(filelists).get().map(
            pkg -> new MatcherOf<Storage>(
                storage -> hasMetadata(storage, temp, pkg, expected),
                desc -> desc.appendText(
                    String.format("Metadata %s has %d rpm packages", pkg.name(), expected)
                ),
                (sto, desc) ->  desc.appendText(
                    String.format(
                        "%d rm packages found for metadata %s",
                        expected, pkg.name()
                    )
                )
            )
        ).collect(Collectors.toList());
    }

    /**
     * Obtains metadata xml from storage and checks that
     * this metadata has correct amount of packages.
     * @param storage Storage
     * @param temp Temp dir to unpack xml
     * @param pckg Package type
     * @param expected Amount of expected items in metadata
     * @return True if metadata is correct
     * @throws Exception On error
     */
    private static boolean hasMetadata(
        final Storage storage, final Path temp, final XmlPackage pckg, final int expected
    ) throws Exception {
        final BlockingStorage bsto = new BlockingStorage(storage);
        final List<Key> repodata = bsto.list(new Key.From("repodata")).stream()
            .filter(key -> key.string().contains(pckg.lowercase())).collect(Collectors.toList());
        final boolean res;
        if (repodata.size() == 1) {
            final Key meta = repodata.get(0);
            final Path gzip = Files.createTempFile(temp, pckg.name(), "xml.gz");
            Files.write(gzip, bsto.value(meta));
            final Path xml = Files.createTempFile(temp, pckg.name(), "xml");
            new Gzip(gzip).unpack(xml);
            res = new NodeHasPkgCount(expected, pckg.tag()).matches(new XMLDocument(xml));
        } else {
            res = false;
        }
        return res;
    }

}

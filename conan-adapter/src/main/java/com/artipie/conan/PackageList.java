/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package  com.artipie.conan;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Lists sub packages of Conan package.
 * @since 0.1
 */
public class PackageList {

    /**
     * Manifest file stores list of package files with their hashes.
     */
    private static final String CONAN_MANIFEST = "conanmanifest.txt";

    /**
     * File with binary package information on corresponding build configuration.
     */
    private static final String CONAN_INFO = "conaninfo.txt";

    /**
     * Main files of package recipe.
     */
    public static final List<String> PKG_SRC_LIST = Collections.unmodifiableList(
        Arrays.asList(
            PackageList.CONAN_MANIFEST, "conan_export.tgz",
            "conanfile.py", "conan_sources.tgz"
        ));

    /**
     * Main files of package binary.
     */
    public static final List<String> PKG_BIN_LIST = Collections.unmodifiableList(
        Arrays.asList(
            PackageList.CONAN_MANIFEST, PackageList.CONAN_INFO, "conan_package.tgz"
        ));

    /**
     * Current Artipie storage instance.
     */
    private final Storage storage;

    /**
     * Initializes new instance.
     * @param storage Current Artipie storage instance.
     */
    public PackageList(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Returns subpackages list of names for given package key.
     * @param pkgkey Full Storage key value for the package.
     * @return CompletionStage with the list of subpackage names as strings.
     */
    public CompletionStage<List<String>> get(final Key pkgkey) {
        return this.storage.list(new Key.From(pkgkey)).thenApply(
            keys -> new ArrayList<>(
                keys.stream().map(key -> PackageList.getNextPart(pkgkey, key))
                    .filter(s -> s.length() > 0).collect(Collectors.toSet())
            )
        );
    }

    /**
     * Extracts next part in key, starting from the base key.
     * @param basekey Base key.
     * @param key Full storage key value.
     * @return Next subpart name after base, or empty string if none.
     */
    private static String getNextPart(final Key basekey, final Key key) {
        final int baselen = basekey.string().length();
        final int next = key.string().indexOf('/', baselen + 1);
        final String result;
        if (next < 0) {
            result = "";
        } else {
            result = key.string().substring(baselen + 1, next);
        }
        return result;
    }
}

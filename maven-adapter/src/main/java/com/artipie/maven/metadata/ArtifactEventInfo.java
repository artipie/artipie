/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.maven.metadata;

import com.artipie.ArtipieException;
import com.artipie.asto.Key;
import com.artipie.maven.http.MavenSlice;
import java.util.Collection;
import java.util.Optional;

/**
 * Helps to obtain and format info for artifact events logging.
 * @since 0.10
 * @checkstyle NonStaticMethodCheck (500 lines)
 */
public final class ArtifactEventInfo {

    /**
     * Supported maven packages: jar, war, pom, maven-plugin, ejb, war, ear, rar, zip.
     * We try to find jar or war package (which is not javadoc and not sources) first, then
     * check for others. If artifact keys list is empty, error is thrown.
     * <a href="https://maven.apache.org/pom.html">Maven docs</a>.
     * @param keys Package item names
     * @return Key to artifact package
     */
    public Key artifactPackage(final Collection<Key> keys) {
        Key result = keys.stream().findFirst().orElseThrow(
            () -> new ArtipieException("No artifact files found")
        );
        for (final String ext : MavenSlice.EXT) {
            final Optional<Key> artifact = keys.stream().filter(
                item -> {
                    final String key = item.string();
                    return key.endsWith(ext) && !key.contains("javadoc")
                        && !key.contains("sources");
                }
            ).findFirst();
            if (artifact.isPresent()) {
                result = artifact.get();
                break;
            }
        }
        return result;
    }

    /**
     * Replaces standard separator of key parts '/' with dot. Expected key is artifact
     * location without version, for example: 'com/artipie/asto'.
     * @param key Artifact location in storage, version not included
     * @return Formatted artifact name
     */
    public String formatArtifactName(final Key key) {
        return this.formatArtifactName(key.string());
    }

    /**
     * Replaces standard separator of key parts '/' with dot. Expected key is artifact
     * location without version, for example: 'com/artipie/asto'.
     * @param key Artifact location in storage, version not included
     * @return Formatted artifact name
     */
    public String formatArtifactName(final String key) {
        return key.replace("/", ".");
    }

}

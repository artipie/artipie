/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget;

import com.artipie.nuget.metadata.Nuspec;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;

/**
 * Package in .nupkg format.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class Nupkg implements NuGetPackage {

    /**
     * Binary content of package.
     */
    private final InputStream content;

    /**
     * Ctor.
     *
     * @param content Binary content of package.
     */
    public Nupkg(final InputStream content) {
        this.content = content;
    }

    @Override
    @SuppressWarnings("PMD.AssignmentInOperand")
    public Nuspec nuspec() {
        Optional<Nuspec> res = Optional.empty();
        try (
            ArchiveInputStream archive = new ArchiveStreamFactory().createArchiveInputStream(
                new BufferedInputStream(this.content)
            )
        ) {
            ArchiveEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                if (!archive.canReadEntryData(entry) || entry.isDirectory()) {
                    continue;
                }
                if (entry.getName().endsWith(".nuspec")) {
                    res = Optional.of(new Nuspec.Xml(archive));
                }
            }
        } catch (final IOException | ArchiveException ex) {
            throw new InvalidPackageException(ex);
        }
        return res.orElseThrow(
            () -> new InvalidPackageException(
                new IllegalArgumentException("No .nuspec file found inside the package.")
            )
        );
    }
}

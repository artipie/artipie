/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.metadata;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

/**
 * NugetRepository search function results.
 * <a href="https://docs.microsoft.com/en-us/nuget/api/search-query-service-resource">Nuget docs</a>.
 * @since 1.2
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UnnecessaryFullyQualifiedName"})
public final class SearchResults {

    /**
     * Output stream to write the result.
     */
    private final OutputStream out;

    /**
     * Ctor.
     * @param out Output stream to write the result
     */
    public SearchResults(final OutputStream out) {
        this.out = out;
    }

    /**
     * Generates search resulting json.
     * @param packages Packages to write results from
     * @throws IOException On IO error
     */
    void generate(final Collection<Package> packages) throws IOException {
        final JsonGenerator gen = new JsonFactory().createGenerator(this.out);
        gen.writeStartObject();
        gen.writeNumberField("totalHits", packages.size());
        gen.writeFieldName("data");
        gen.writeStartArray();
        for (final Package item : packages) {
            gen.writeStartObject();
            gen.writeStringField("id", item.id);
            gen.writeStringField("version", item.version());
            gen.writeFieldName("packageTypes");
            gen.writeArray(item.types.toArray(new String[]{}), 0, item.types.size());
            gen.writeFieldName("versions");
            gen.writeStartArray();
            for (final Version vers : item.versions) {
                vers.write(gen);
            }
            gen.writeEndArray();
            gen.writeEndObject();
        }
        gen.writeEndArray();
        gen.close();
    }

    /**
     * Package info. Check
     * <a href="https://docs.microsoft.com/en-us/nuget/api/search-query-service-resource#search-result">NuGet docs</a>
     * for full description.
     * @since 1.2
     */
    public static final class Package {

        /**
         * Package id.
         */
        private final String id;

        /**
         * The package types defined by the package author.
         */
        private final Collection<String> types;

        /**
         * Package versions.
         */
        private final Collection<Version> versions;

        /**
         * Ctor.
         * @param id Package id
         * @param types Package types
         * @param versions Package versions
         */
        public Package(final String id, final Collection<String> types,
            final Collection<Version> versions) {
            this.id = id;
            this.types = types;
            this.versions = versions;
        }

        /**
         * Get latest version from versions array.
         * @return Version
         */
        String version() {
            return this.versions.stream()
                .map(vers -> new com.artipie.nuget.metadata.Version(vers.value))
                .max(com.artipie.nuget.metadata.Version::compareTo).get().normalized();
        }
    }

    /**
     * Package version.
     * @since 1.2
     */
    public static final class Version {

        /**
         * The full SemVer 2.0.0 version string of the package.
         */
        private final String value;

        /**
         * The number of downloads for this specific package version.
         */
        private final long downloads;

        /**
         * The absolute URL to the associated registration leaf.
         */
        private final String id;

        /**
         * Ctor.
         * @param value The full SemVer 2.0.0 version string of the package
         * @param downloads The number of downloads for this specific package version
         * @param id The absolute URL to the associated registration leaf
         */
        Version(final String value, final long downloads, final String id) {
            this.value = value;
            this.downloads = downloads;
            this.id = id;
        }

        /**
         * Writes itself to {@link JsonGenerator}.
         * @param gen Where to write
         * @throws IOException On IO error
         */
        private void write(final JsonGenerator gen) throws IOException {
            gen.writeStartObject();
            gen.writeStringField(
                "version", new com.artipie.nuget.metadata.Version(this.value).normalized()
            );
            gen.writeNumberField("downloads", this.downloads);
            gen.writeStringField("@id", this.id);
            gen.writeEndObject();
        }
    }

}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.cactoos.list.ListOf;

/**
 * Maven artifact metadata xml.
 */
public final class MetadataXml {

    /**
     * Group id.
     */
    private final String group;

    /**
     * Artifact id.
     */
    private final String artifact;

    /**
     * Ctor.
     * @param group Group id
     * @param artifact Artifact id
     */
    public MetadataXml(final String group, final String artifact) {
        this.group = group;
        this.artifact = artifact;
    }

    /**
     * Adds xml to storage.
     * @param storage Where to add
     * @param key Key to save xml by
     * @param versions Version to generage xml
     */
    public void addXmlToStorage(final Storage storage, final Key key, final VersionTags versions) {
        storage.save(key, new Content.From(this.get(versions).getBytes(StandardCharsets.UTF_8)))
            .join();
    }

    /**
     * Get xml as string.
     * @param versions Versions info
     * @return Maven metadata xml
     */
    public String get(final VersionTags versions) {
        return String.join(
            "\n",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<metadata>",
            String.format("  <groupId>%s</groupId>", this.group),
            String.format("  <artifactId>%s</artifactId>", this.artifact),
            "  <versioning>",
            versions.latest.map(val -> String.format("    <latest>%s</latest>", val)).orElse(""),
            versions.release.map(val -> String.format("    <release>%s</release>", val)).orElse(""),
            "    <versions>",
            versions.list.stream().map(val -> String.format("      <version>%s</version>", val))
                .collect(Collectors.joining("\n")),
            "    </versions>",
            "    <lastUpdated>20200804141716</lastUpdated>",
            "  </versioning>",
            "</metadata>"
        );
    }

    /**
     * Maven metadata tags with versions: latest, release, versions list.
     */
    public static final class VersionTags {

        /**
         * Latest version.
         */
        private final Optional<String> latest;

        /**
         * Release version.
         */
        private final Optional<String> release;

        /**
         * Versions list.
         */
        private final List<String> list;

        /**
         * Ctor.
         * @param latest Latest version
         * @param release Release version
         * @param list Versions list
         */
        public VersionTags(final Optional<String> latest, final Optional<String> release,
            final List<String> list) {
            this.latest = latest;
            this.release = release;
            this.list = list;
        }

        /**
         * Ctor.
         * @param latest Latest version
         * @param release Release version
         * @param list Versions list
         */
        public VersionTags(final String latest, final String release, final List<String> list) {
            this(Optional.of(latest), Optional.of(release), list);
        }

        /**
         * Ctor.
         * @param list Versions list
         */
        public VersionTags(final String... list) {
            this(Optional.empty(), Optional.empty(), new ListOf<>(list));
        }
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * CatalogEntry item from registration page.
 * <a href="https://learn.microsoft.com/en-us/nuget/api/registration-base-url-resource#catalog-entry">Docs</a>.
 * @since 1.5
 */
public interface CatalogEntry {

    /**
     * CatalogEntry as {@link JsonObject}.
     * @return Json representation of CatalogEntry
     */
    JsonObject asJson();

    /**
     * Creates CatalogEntry from nuspec metadata.
     * @since 1.5
     */
    final class FromNuspec implements CatalogEntry {

        /**
         * The name of the `authors` json field.
         */
        private static final String AUTHORS_FIELD = "authors";

        /**
         * Nuspec metadata.
         */
        private final Nuspec nuspec;

        /**
         * Ctor.
         * @param nuspec Nuspec metadata
         */
        public FromNuspec(final Nuspec nuspec) {
            this.nuspec = nuspec;
        }

        @Override
        public JsonObject asJson() {
            final JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("@id", "null");
            builder.add("id", this.nuspec.id().raw());
            builder.add("version", this.nuspec.version().raw());
            builder.add("description", this.nuspec.description());
            final String authors = this.nuspec.authors();
            if (authors.contains(",")) {
                final JsonArrayBuilder arr = Json.createArrayBuilder();
                Arrays.stream(authors.split(",")).forEach(item -> arr.add(item.trim()));
                builder.add(FromNuspec.AUTHORS_FIELD, arr);
            } else {
                builder.add(FromNuspec.AUTHORS_FIELD, authors);
            }
            builder.add("dependencyGroups", this.dependencyGroupArray());
            this.addOptFields(builder);
            return builder.build();
        }

        /**
         * Adds optional fields from nuspec into catalog entry. Note, that `licenceUrl` and
         * `iconUrl` were replaced with `license` and `icon` correspondingly in the newest versions,
         * but in the catalogEntry fields are called `licenceUrl` and `iconUrl` (with `url` part).
         * For the other fields, the names in nuspec and catalogEntry match.
         * About `tags` check
         * <a href="https://learn.microsoft.com/en-us/nuget/reference/nuspec#tags">docs</a>.
         * @param builder Json object builder to add fields to
         */
        private void addOptFields(final JsonObjectBuilder builder) {
            this.nuspec.minClientVersion().ifPresent(min -> builder.add("minClientVersion", min));
            this.addOneOfTwo(builder, OptFieldName.LICENSE, OptFieldName.LICENSE_URL);
            this.addOneOfTwo(builder, OptFieldName.ICON, OptFieldName.ICON_URL);
            Stream.of(
                OptFieldName.PROJECT_URL, OptFieldName.REQUIRE_LICENSE_ACCEPTANCE,
                OptFieldName.TITLE, OptFieldName.SUMMARY
            ).forEach(
                item -> this.nuspec.fieldByName(item).ifPresent(val -> builder.add(item.get(), val))
            );
            this.nuspec.fieldByName(OptFieldName.TAGS).ifPresent(
                val -> {
                    if (val.contains(" ")) {
                        final JsonArrayBuilder arr = Json.createArrayBuilder();
                        Stream.of(val.split(" ")).forEach(arr::add);
                        builder.add(OptFieldName.TAGS.get(), arr);
                    } else {
                        builder.add(OptFieldName.TAGS.get(), val);
                    }
                }
            );
        }

        /**
         * Add one of two possible field values into builder. Second field name is used
         * in the builder.
         * @param builder Json object builder
         * @param first First field name
         * @param second Second field name
         */
        private void addOneOfTwo(final JsonObjectBuilder builder, final OptFieldName first,
            final OptFieldName second) {
            Stream.of(this.nuspec.fieldByName(first), this.nuspec.fieldByName(second))
                .filter(Optional::isPresent).map(Optional::get).findFirst()
                .ifPresent(lc -> builder.add(second.get(), lc));
        }

        /**
         * Builds dependency group array. To learn more about the format, check the
         * <a href="https://learn.microsoft.com/en-us/nuget/api/registration-base-url-resource#package-dependency-group">docs</a>.
         * @return Instance of {@link JsonArrayBuilder} with dependency groups.
         */
        private JsonArrayBuilder dependencyGroupArray() {
            final JsonArrayBuilder groups = Json.createArrayBuilder();
            this.dependenciesByTargetFramework().forEach(
                (key, val) -> {
                    final JsonObjectBuilder item = Json.createObjectBuilder();
                    if (!key.isEmpty()) {
                        item.add("targetFramework", key);
                    }
                    final JsonArrayBuilder deps = Json.createArrayBuilder();
                    val.forEach(
                        pair -> {
                            final JsonObjectBuilder dep = Json.createObjectBuilder();
                            dep.add("id", pair.getKey());
                            if (!pair.getValue().isEmpty()) {
                                dep.add("range", String.format("[%s, )", pair.getValue()));
                            }
                            deps.add(dep);
                        }
                    );
                    groups.add(item.add("dependencies", deps));
                }
            );
            return groups;
        }

        /**
         * Dependencies grouped by target framework. Dependencies, which do not have the
         * target framework, are placed with empty string key. {@link Nuspec#dependencies()}
         * returns deps in the following format:
         * <code>dependency_id:dependency_version:group_targetFramework</code>
         * The last part `group_targetFramework` can be empty.
         * @return Dependencies grouped by target framework
         * @checkstyle MagicNumberCheck (20 lines)
         */
        private Map<String, List<Pair<String, String>>> dependenciesByTargetFramework() {
            final Map<String, List<Pair<String, String>>> res = new HashMap<>();
            this.nuspec.dependencies().forEach(
                item -> {
                    final String[] arr = item.split(":");
                    final List<Pair<String, String>> dep;
                    if (arr[0].isEmpty()) {
                        dep = Collections.emptyList();
                    } else {
                        dep = new ArrayList<>(12);
                        dep.add(new ImmutablePair<>(arr[0], arr[1]));
                    }
                    final String framework;
                    if (arr.length == 3) {
                        framework = arr[2];
                    } else {
                        framework = "";
                    }
                    res.merge(
                        framework, dep,
                        (existing, adding) -> {
                            existing.addAll(adding);
                            return existing;
                        }
                    );
                }
            );
            return res;
        }
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.nuget.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Nuget package dependencies groups.
 * <a href="https://docs.microsoft.com/en-us/nuget/api/registration-base-url-resource#package-dependency-group">Documentation.</a>
 * @since 0.8
 */
public interface DependencyGroups {

    /**
     * Build dependencies groups json array.
     * @return Json format of dependencies groups
     */
    JsonArray build();

    /**
     * Builds DependencyGroups json from ist of the dependencies formatted as
     * <code>dependency_id:dependency_version:group_targetFramework</code>.
     * Note, that any part from dependency can be absent, for example the
     * following variants are legal:
     * ::SomeFramework
     * AbcPackage::AbcFramework
     * XyzPackage:0.7:
     * @since 0.8
     */
    class FromVersions implements DependencyGroups {

        /**
         * Max parsed dependency size.
         */
        private static final int MAX_SIZE = 3;

        /**
         * Versions list.
         */
        private final Collection<String> versions;

        /**
         * Ctor.
         * @param versions Versions list
         */
        public FromVersions(final Collection<String> versions) {
            this.versions = versions;
        }

        @Override
        public JsonArray build() {
            final Map<String, List<Pair<String, String>>> items =
                new HashMap<>(this.versions.size());
            for (final String item : this.versions) {
                final String framework = FromVersions.getFramework(item);
                items.compute(
                    framework, (key, val) -> {
                        List<Pair<String, String>> res = val;
                        if (res == null) {
                            res = new ArrayList<>(this.versions.size());
                        }
                        res.add(
                            new ImmutablePair<>(
                                FromVersions.getId(item), FromVersions.getVersion(item)
                            )
                        );
                        return res;
                    }
                );
            }
            final JsonArrayBuilder array = Json.createArrayBuilder();
            for (final Map.Entry<String, List<Pair<String, String>>> entry : items.entrySet()) {
                final JsonObjectBuilder builder = Json.createObjectBuilder();
                final JsonArrayBuilder arr = Json.createArrayBuilder();
                entry.getValue().forEach(
                    pair -> arr.add(
                        Json.createObjectBuilder().add("id", pair.getKey())
                            .add("range", pair.getValue()).build()
                    )
                );
                builder.add("dependencies", arr);
                builder.add("targetFramework", entry.getKey());
                array.add(builder);
            }
            return array.build();
        }

        @Override
        public String toString() {
            return this.build().toString();
        }

        /**
         * Calculate version from item string.
         * @param item Full item
         * @return Version
         */
        private static String getVersion(final String item) {
            final String[] arr = item.split(":");
            final String version;
            if (arr.length == FromVersions.MAX_SIZE || arr.length == 2 && item.endsWith(":")) {
                version = arr[1];
            } else if (item.charAt(0) == ':') {
                version = arr[0];
            } else {
                version = "";
            }
            return version;
        }

        /**
         * Calculate id from item string.
         * @param item Full item
         * @return Id
         */
        private static String getId(final String item) {
            final String[] arr = item.split(":");
            final String id;
            if (arr.length == FromVersions.MAX_SIZE || arr.length == 2 && item.endsWith(":")) {
                id = arr[0];
            } else {
                id = "";
            }
            return id;
        }

        /**
         * Calculate framework from item string.
         * @param item Full item
         * @return Framework
         */
        private static String getFramework(final String item) {
            final String[] arr = item.split(":");
            final String framework;
            if (arr.length == FromVersions.MAX_SIZE) {
                framework = arr[2];
            } else if (item.endsWith(":")) {
                framework = "";
            } else {
                framework = arr[1];
            }
            return framework;
        }
    }

}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget;

import com.artipie.nuget.metadata.CatalogEntry;
import com.artipie.nuget.metadata.Nuspec;
import com.artipie.nuget.metadata.PackageId;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * Index json operations. Index json is metadata file for various version of NuGet package, it's
 * called registration page in the repository docs.
 * <a href="https://learn.microsoft.com/en-us/nuget/api/registration-base-url-resource#registration-page-object">Registration page</a>.
 * @since 1.5
 * @checkstyle InterfaceIsTypeCheck (500 lines)
 */
public abstract class IndexJson {

    /**
     * Default null value for index.json required fields with urls values.
     */
    private static final String NULL = "null";

    /**
     * The name of the `@id` json field.
     */
    private static final String ID = "@id";

    /**
     * The name of the `items` json field.
     */
    private static final String ITEMS = "items";

    /**
     * The name of the `catalogEntry` json field.
     */
    private static final String CATALOG_ENTRY = "catalogEntry";

    /**
     * The name of the `count` json field.
     */
    private static final String COUNT = "count";

    /**
     * The name of the `lower` json field.
     */
    private static final String LOWER = "lower";

    /**
     * The name of the `upper` json field.
     */
    private static final String UPPER = "upper";

    /**
     * Add `@id` and `count` fields into resulting json object builder.
     * @param res Resulting json object builder
     * @param id Field value `@id`
     * @param cnt Count field value
     */
    private static void addIdAndCount(final JsonObjectBuilder res, final String id, final int cnt) {
        res.add(IndexJson.ID, id);
        res.add(IndexJson.COUNT, cnt);
    }

    /**
     * Obtain package version from json value item.
     * @param val Json Value item
     * @return String version
     */
    private static String version(final JsonObject val) {
        return val.getJsonObject(IndexJson.CATALOG_ENTRY).getString("version");
    }

    /**
     * Obtain "items" json that contains "catalogEntry" items, the structure is the following:
     * {
     *   "count": 1,
     *   "items": [
     *     {
     *       "@id": "https://...",
     *       "count": 1,
     *       "lower": "1.3.8",
     *       "upper": "5.0.7",
     *       "items": [ ... ]
     *     }
     *   ]
     * }
     * Internal "items" array is what we obtain here.
     * @param source Json source
     * @return Json "items" array
     */
    private static Optional<JsonArray> itemsJsonArray(final JsonObject source) {
        Optional<JsonArray> res = Optional.empty();
        if (source.containsKey(IndexJson.ITEMS)
            && !source.getJsonArray(IndexJson.ITEMS).isEmpty()) {
            final JsonObject obj = source.getJsonArray(IndexJson.ITEMS).get(0).asJsonObject();
            if (obj.containsKey(IndexJson.ITEMS)
                && !obj.getJsonArray(IndexJson.ITEMS).isEmpty()) {
                res = Optional.of(obj.getJsonArray(IndexJson.ITEMS));
            }
        }
        return res;
    }

    /**
     * Delete nuget package by name and version from index.json.
     * @since 1.6
     */
    public static final class Delete extends IndexJson {

        /**
         * Input stream with existing index json metadata.
         */
        private final InputStream input;

        /**
         * Ctor.
         * @param input Input stream with existing index json metadata
         */
        public Delete(final InputStream input) {
            this.input = input;
        }

        /**
         * Perform delete operation.
         * @param name The name of the package
         * @param version Package version
         * @return Json object with the index without removed package
         */
        public JsonObject perform(final String name, final String version) {
            final JsonObjectBuilder res = Json.createObjectBuilder();
            final Optional<JsonArray> array = itemsJsonArray(
                Json.createReader(this.input).readObject()
            );
            List<JsonObject> items = Collections.emptyList();
            if (array.isPresent()) {
                items = array.get().stream().map(JsonValue::asJsonObject)
                    .filter(
                        item -> {
                            final JsonObject entry = item.getJsonObject(IndexJson.CATALOG_ENTRY);
                            return !(new PackageId(entry.getString("id")).normalized()
                                .equals(new PackageId(name).normalized())
                                && new ComparableVersion(version(item))
                                .equals(new ComparableVersion(version)));
                        }
                    ).sorted(Comparator.comparing(val -> new ComparableVersion(version(val))))
                        .collect(Collectors.toList());
            }
            if (!items.isEmpty()) {
                final JsonObjectBuilder jitems = Json.createObjectBuilder();
                jitems.add(IndexJson.LOWER, version(items.get(0)));
                jitems.add(IndexJson.UPPER, version(items.get(items.size() - 1)));
                addIdAndCount(jitems, IndexJson.NULL, items.size());
                final JsonArrayBuilder builder = Json.createArrayBuilder();
                items.forEach(builder::add);
                jitems.add(IndexJson.ITEMS, builder);
                res.add(IndexJson.COUNT, 1)
                    .add(IndexJson.ITEMS, Json.createArrayBuilder().add(jitems));
            }
            return res.build();
        }
    }

    /**
     * Update (or create) index.json metadata by adding
     * package info from NUSPEC package metadata.
     * @since 1.5
     */
    public static final class Update extends IndexJson {

        /**
         * Optional input stream with existing index json metadata.
         */
        private final Optional<InputStream> input;

        /**
         * Primary ctor.
         *
         * @param input Optional input stream with existing index json metadata
         */
        public Update(final Optional<InputStream> input) {
            this.input = input;
        }

        /**
         * Ctor with empty optional for input stream.
         */
        public Update() {
            this(Optional.empty());
        }

        /**
         * Ctor.
         *
         * @param input Optional input stream with existing index json metadata
         */
        public Update(final InputStream input) {
            this(Optional.of(input));
        }

        /**
         * Creates or updates index.json by adding information about new provided package. If such
         * package version already exists in index.json, package metadata are replaced.
         * {@link  NuGetPackage} instance can be created from NuGet package input stream by calling
         * constructor {@link Nupkg#Nupkg(InputStream)}.
         * In the resulting json object catalogEntries are placed in the ascending order by
         * package version. Required url fields (like @id, packageContent, @id of the catalogEntry)
         * are set to "null" string.
         *
         * @param pkg New package to add
         * @return Updated index.json metadata as {@link JsonObject}
         */
        public JsonObject perform(final NuGetPackage pkg) {
            final JsonObjectBuilder res = Json.createObjectBuilder();
            final Nuspec nuspec = pkg.nuspec();
            final JsonObject newest = newPackageJsonItem(nuspec);
            final String version = nuspec.version().normalized();
            final JsonArrayBuilder itemsbuilder = Json.createArrayBuilder();
            if (this.input.isPresent()) {
                final JsonObject old = Json.createReader(this.input.get()).readObject();
                final List<JsonObject> list = sortedPackages(newest, version, old);
                list.forEach(itemsbuilder::add);
                addIdAndCount(res, old.getString(IndexJson.ID, IndexJson.NULL), list.size());
            } else {
                itemsbuilder.add(newest);
                addIdAndCount(res, IndexJson.NULL, 1);
            }
            final JsonArray items = itemsbuilder.build();
            res.add(IndexJson.UPPER, version(items.get(items.size() - 1).asJsonObject()));
            res.add(IndexJson.LOWER, version(items.get(0).asJsonObject()));
            res.add(IndexJson.ITEMS, items);
            return Json.createObjectBuilder().add(IndexJson.COUNT, 1)
                .add(IndexJson.ITEMS, Json.createArrayBuilder().add(res)).build();
        }

        /**
         * Here we check if the existing packages metadata array is present and not empty and if it
         * already contains metadata for new package version, replace it if it does or simply add
         * new package meta if it does not, then we sort packages meta by package version
         * and return sorted list.
         * @param newest New package metadata in json format
         * @param version Version of new package
         * @param old Existing packages metadata array
         * @return Sorted by packages version list of the packages metadata including new package
         * @checkstyle InnerAssignmentCheck (10 lines)
         */
        @SuppressWarnings("PMD.AssignmentInOperand")
        private static List<JsonObject> sortedPackages(final JsonObject newest,
            final String version, final JsonObject old) {
            List<JsonObject> list = Collections.singletonList(newest);
            final Optional<JsonArray> array = itemsJsonArray(old);
            if (array.isPresent()) {
                final JsonArray arr = array.get();
                list = new ArrayList<>(arr.size() + 1);
                arr.stream().map(JsonValue::asJsonObject)
                    .filter(val -> !version.equals(version(val))).forEach(list::add);
                list.add(newest);
                list.sort(Comparator.comparing(val -> new ComparableVersion(version(val))));
            }
            return list;
        }

        /**
         * Build json item for new package.
         * @param nuspec New package to add
         * @return Json object of the new package
         */
        private static JsonObject newPackageJsonItem(final Nuspec nuspec) {
            return Json.createObjectBuilder()
                .add(IndexJson.ID, IndexJson.NULL).add("packageContent", IndexJson.NULL)
                .add(IndexJson.CATALOG_ENTRY, new CatalogEntry.FromNuspec(nuspec).asJson()).build();
        }
    }
}

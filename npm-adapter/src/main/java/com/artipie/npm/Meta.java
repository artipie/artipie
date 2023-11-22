/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.npm.misc.DateTimeNowStr;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;
import javax.json.JsonValue;

/**
 * The meta.json file.
 *
 * @since 0.1
 * @checkstyle ExecutableStatementCountCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class Meta {
    /**
     * Latest tag name.
     */
    static final String LATEST = "latest";

    /**
     * The meta.json file.
     */
    private final JsonObject json;

    /**
     * Ctor.
     *
     * @param json The meta.json file location on disk.
     */
    Meta(final JsonObject json) {
        this.json = json;
    }

    /**
     * Update the meta.json file by processing newly
     * uploaded {@code npm publish} generated json.
     *
     * @param uploaded The json
     * @return Completion or error signal.
     */
    public JsonObject updatedMeta(final JsonObject uploaded) {
        boolean haslatest = false;
        final JsonObject versions = uploaded.getJsonObject("versions");
        final Set<String> keys = versions.keySet();
        final JsonPatchBuilder patch = Json.createPatchBuilder();
        if (this.json.containsKey("dist-tags")) {
            haslatest = this.json.getJsonObject("dist-tags").containsKey(Meta.LATEST);
        } else {
            patch.add("/dist-tags", Json.createObjectBuilder().build());
        }
        for (final Map.Entry<String, JsonValue> tag
            : uploaded.getJsonObject("dist-tags").entrySet()
        ) {
            patch.add(String.format("/dist-tags/%s", tag.getKey()), tag.getValue());
            if (tag.getKey().equals(Meta.LATEST)) {
                haslatest = true;
            }
        }
        for (final String key : keys) {
            final JsonObject version = versions.getJsonObject(key);
            patch.add(
                String.format("/versions/%s", key),
                version
            );
            patch.add(
                String.format("/versions/%s/dist/tarball", key),
                String.format(
                    "/%s",
                    new TgzRelativePath(version.getJsonObject("dist").getString("tarball"))
                        .relative()
                )
            );
        }
        final String now = new DateTimeNowStr().value();
        for (final String version : keys) {
            patch.add(String.format("/time/%s", version), now);
        }
        patch.add("/time/modified", now);
        if (!haslatest && !keys.isEmpty()) {
            final List<String> lst = new ArrayList<>(keys);
            lst.sort(Comparator.reverseOrder());
            patch.add("/dist-tags/latest", lst.get(0));
        }
        return patch.build().apply(this.json);
    }
}

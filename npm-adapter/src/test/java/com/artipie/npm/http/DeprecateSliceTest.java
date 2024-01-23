/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.npm.JsonFromMeta;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import wtf.g4s8.hamcrest.json.JsonHas;
import wtf.g4s8.hamcrest.json.JsonValueIs;

/**
 * Test for {@link DeprecateSlice}.
 * @since 0.8
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class DeprecateSliceTest {

    /**
     * Deprecated field name.
     */
    private static final String FIELD = "deprecated";

    /**
     * Test project name.
     */
    private static final String PROJECT = "@hello/simple-npm-project";

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Meta file key.
     */
    private Key meta;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.meta = new Key.From(DeprecateSliceTest.PROJECT, "meta.json");
    }

    @Test
    void addsDeprecateFieldForVersion() {
        this.storage.save(this.meta, this.createMetaJson(false)).join();
        final String value = "This version is deprecated!";
        MatcherAssert.assertThat(
            "Response status is OK",
            new DeprecateSlice(this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(
                    RqMethod.PUT, "/@hello%2fsimple-npm-project"
                ),
                Headers.EMPTY,
                new Content.From(
                    Json.createObjectBuilder()
                        .add("name", DeprecateSliceTest.PROJECT)
                        .add(
                            "versions",
                            Json.createObjectBuilder().add(
                                "1.0.1",
                                Json.createObjectBuilder()
                                    .add("name", DeprecateSliceTest.PROJECT)
                                    .add("version", "1.0.1")
                            ).add(
                                "1.0.2",
                                Json.createObjectBuilder()
                                    .add("name", DeprecateSliceTest.PROJECT)
                                    .add("version", "1.0.2")
                                    .add(DeprecateSliceTest.FIELD, value)
                            )
                        ).build().toString().getBytes(StandardCharsets.UTF_8)
                )
            )
        );
        MatcherAssert.assertThat(
            "Meta.json is updated",
            new JsonFromMeta(this.storage, new Key.From(DeprecateSliceTest.PROJECT)).json(),
            Matchers.allOf(
                new JsonHas(
                    "versions",
                    new JsonHas(
                        "1.0.2",
                        new JsonHas(DeprecateSliceTest.FIELD, new JsonValueIs(value))
                    )
                ),
                new JsonHas(
                    "versions",
                    new JsonHas(
                        "1.0.1",
                        new IsNot<>(new JsonHas(DeprecateSliceTest.FIELD, new JsonValueIs(value)))
                    )
                )
            )
        );
    }

    @Test
    void addsDeprecateFieldForVersions() {
        this.storage.save(this.meta, this.createMetaJson(false)).join();
        final String value = "Do not use!";
        MatcherAssert.assertThat(
            "Response status is OK",
            new DeprecateSlice(this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(
                    RqMethod.PUT, "/@hello%2fsimple-npm-project"
                ),
                Headers.EMPTY,
                new Content.From(
                    Json.createObjectBuilder()
                        .add("name", DeprecateSliceTest.PROJECT)
                        .add(
                            "versions",
                            Json.createObjectBuilder().add(
                                "1.0.1",
                                Json.createObjectBuilder()
                                    .add("name", DeprecateSliceTest.PROJECT)
                                    .add("version", "1.0.1")
                                    .add(DeprecateSliceTest.FIELD, value)
                            ).add(
                                "1.0.2",
                                Json.createObjectBuilder()
                                    .add("name", DeprecateSliceTest.PROJECT)
                                    .add("version", "1.0.2")
                                    .add(DeprecateSliceTest.FIELD, value)
                            )
                        ).build().toString().getBytes(StandardCharsets.UTF_8)
                )
            )
        );
        MatcherAssert.assertThat(
            "Meta.json is updated",
            new JsonFromMeta(this.storage, new Key.From(DeprecateSliceTest.PROJECT)).json(),
            Matchers.allOf(
                new JsonHas(
                    "versions",
                    new JsonHas(
                        "1.0.2", new JsonHas(DeprecateSliceTest.FIELD, new JsonValueIs(value))
                    )
                ),
                new JsonHas(
                    "versions",
                    new JsonHas(
                        "1.0.1", new JsonHas(DeprecateSliceTest.FIELD, new JsonValueIs(value))
                    )
                )
            )
        );
    }

    @Test
    void deprecatedFieldShouldBeRemovedByEmptyMessage() {
        final String msg = "";
        this.storage.save(this.meta, this.createMetaJson(true)).join();
        MatcherAssert.assertThat(
            "Response status is OK",
            new DeprecateSlice(this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(
                    RqMethod.PUT, "/@hello%2fsimple-npm-project"
                ),
                Headers.EMPTY,
                new Content.From(
                    Json.createObjectBuilder()
                        .add("name", DeprecateSliceTest.PROJECT)
                        .add(
                            "versions", Json.createObjectBuilder()
                        .add(
                            "1.0.3",
                            Json.createObjectBuilder()
                                .add("name", DeprecateSliceTest.PROJECT)
                                .add("version", "1.0.3")
                                .add("deprecated", msg)
                            )
                        )
                    .build().toString().getBytes(StandardCharsets.UTF_8)
                )
            )
        );
        MatcherAssert.assertThat(
            "Meta.json is updated",
            new JsonFromMeta(this.storage, new Key.From(DeprecateSliceTest.PROJECT)).json()
                .getJsonObject("versions")
                .getJsonObject("1.0.3")
                .getJsonString(DeprecateSliceTest.FIELD),
            new IsNull<>()
        );
    }

    @Test
    void returnsNotFoundIfMetaIsNotFound() {
        MatcherAssert.assertThat(
            new DeprecateSlice(this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NOT_FOUND),
                new RequestLine(RqMethod.PUT, "/some/project")
            )
        );
    }

    private Content createMetaJson(final boolean third) {
        final JsonObjectBuilder versions =
            Json.createObjectBuilder().add(
                "1.0.1", Json.createObjectBuilder()
                    .add("name", DeprecateSliceTest.PROJECT)
                    .add("version", "1.0.1")
            ).add(
                "1.0.2",
                Json.createObjectBuilder()
                    .add("name", DeprecateSliceTest.PROJECT)
                    .add("version", "1.0.2")
            );
        if (third) {
            versions.add(
                "1.0.3",
                Json.createObjectBuilder()
                    .add("name", DeprecateSliceTest.PROJECT)
                    .add("version", "1.0.3")
                    .add("deprecated", "Some deprecated message")
            );
        }
        return new Content.From(
            Json.createObjectBuilder()
                .add("name", DeprecateSliceTest.PROJECT)
                .add("versions", versions)
                .build().toString().getBytes(StandardCharsets.UTF_8)
        );
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.nuget;

import com.artipie.asto.test.TestResource;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link IndexJson.Delete}.
 * @since 1.6
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class IndexJsonDeleteTest {

    /**
     * Test index.json.
     */
    private TestResource resource;

    @BeforeEach
    void init() {
        this.resource = new TestResource("IndexJsonDeleteTest/three_versions.json");
    }

    @Test
    void deletesMiddleItemIgnoringIdCase() throws JSONException {
        JSONAssert.assertEquals(
            new IndexJson.Delete(this.resource.asInputStream())
                .perform("neWtonsOft.json", "12.0.3").toString(),
            new String(
                new TestResource("IndexJsonDeleteTest/deletesMiddleItem_res.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            true
        );
    }

    @Test
    void deletesFirstItem() throws JSONException {
        JSONAssert.assertEquals(
            new IndexJson.Delete(this.resource.asInputStream())
                .perform("newtonsoft.json", "0.13.1").toString(),
            new String(
                new TestResource("IndexJsonDeleteTest/deletesFirstItem_res.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            true
        );
    }

    @Test
    void deletesLastItem() throws JSONException {
        JSONAssert.assertEquals(
            new IndexJson.Delete(this.resource.asInputStream())
                .perform("newtonsoft.json", "13.0.1").toString(),
            new String(
                new TestResource("IndexJsonDeleteTest/deletesLastItem_res.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            true
        );
    }

    @Test
    void doesNothingIfItemNotFound() throws JSONException {
        JSONAssert.assertEquals(
            new IndexJson.Delete(this.resource.asInputStream())
                .perform("somePackage", "1.0.0").toString(),
            new String(
                this.resource.asBytes(),
                StandardCharsets.UTF_8
            ),
            true
        );
    }

    @Test
    void returnsEmptyJsonWhenLastItemRemoved() {
        MatcherAssert.assertThat(
            new IndexJson.Delete(
                new TestResource("IndexJsonDeleteTest/one_version.json").asInputStream()
            ).perform("newtonsoft.json", "0.13.1").isEmpty(),
            new IsEqual<>(true)
        );
    }

}

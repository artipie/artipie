/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.nuget;

import com.artipie.asto.test.TestResource;
import java.nio.charset.StandardCharsets;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link IndexJson.Update}.
 * @since 1.5
 */
class IndexJsonUpdateTest {

    /**
     * Test nuget package.
     */
    private Nupkg pkg;

    @BeforeEach
    void init() {
        this.pkg = new Nupkg(
            new TestResource("IndexJsonUpdateTest/newtonsoft.json.12.0.3.nupkg").asInputStream()
        );
    }

    @Test
    void createsIndexJson() throws JSONException {
        JSONAssert.assertEquals(
            new String(
                new TestResource("IndexJsonUpdateTest/createsIndexJson_res.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            new IndexJson.Update().perform(this.pkg).toString(),
            true
        );
    }

    @Test
    void updatesIndexJsonWithOneVersion() throws JSONException {
        JSONAssert.assertEquals(
            new String(
                new TestResource("IndexJsonUpdateTest/updatesIndexJsonWithOneVersion_res.json")
                    .asBytes(),
                StandardCharsets.UTF_8
            ),
            new IndexJson.Update(
                new TestResource("IndexJsonUpdateTest/index_newtonsoft_13.0.1.json").asInputStream()
            ).perform(this.pkg).toString(),
            true
        );
    }

    @Test
    void updatesIndexJsonWithThreeVersion() throws JSONException {
        JSONAssert.assertEquals(
            new String(
                new TestResource("IndexJsonUpdateTest/updatesIndexJsonWithThreeVersion_res.json")
                    .asBytes(),
                StandardCharsets.UTF_8
            ),
            new IndexJson.Update(
                new TestResource("IndexJsonUpdateTest/index_newtonsoft_three_versions.json")
                    .asInputStream()
            ).perform(this.pkg).toString(),
            true
        );
    }

    @Test
    void replacesExistingVersionItem() throws JSONException {
        JSONAssert.assertEquals(
            new String(
                new TestResource("IndexJsonUpdateTest/replacesExistingVersionItem_res.json")
                    .asBytes(),
                StandardCharsets.UTF_8
            ),
            new IndexJson.Update(
                new TestResource("IndexJsonUpdateTest/index_newtonsoft_replace.json")
                    .asInputStream()
            ).perform(this.pkg).toString(),
            true
        );
    }

    @Test
    void updatesIndexWithEmptyItems() throws JSONException {
        JSONAssert.assertEquals(
            new String(
                new TestResource("IndexJsonUpdateTest/updatesIndexWithEmptyItems_res.json")
                    .asBytes(),
                StandardCharsets.UTF_8
            ),
            new IndexJson.Update(
                new TestResource("IndexJsonUpdateTest/index_empty_items.json")
                    .asInputStream()
            ).perform(this.pkg).toString(),
            true
        );
    }

}

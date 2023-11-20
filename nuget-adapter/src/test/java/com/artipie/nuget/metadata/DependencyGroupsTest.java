/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.metadata;

import com.artipie.asto.test.TestResource;
import com.google.common.collect.Lists;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import org.json.JSONException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link DependencyGroups.FromVersions}.
 * @since 0.8
 */
class DependencyGroupsTest {

    @ParameterizedTest
    @CsvSource({
        "one:0.1:AnyFramework;two:0.2:AnyFramework;another:0.1:anotherFrameWork,json_res1.json",
        "abc:0.1:ABCFramework;xyz:0.0.1:;def:0.1:,json_res2.json",
        "::EmptyFramework;xyz:0.0.1:XyzFrame;def::DefFrame,json_res3.json"
    })
    void buildsJson(final String list, final String res) throws JSONException {
        JSONAssert.assertEquals(
            Json.createObjectBuilder().add(
                "DependencyGroups",
                new DependencyGroups.FromVersions(Lists.newArrayList(list.split(";"))).build()
            ).build().toString(),
            new String(
                new TestResource(String.format("DependencyGroupsTest/%s", res)).asBytes(),
                StandardCharsets.UTF_8
            ),
            true
        );
    }

}

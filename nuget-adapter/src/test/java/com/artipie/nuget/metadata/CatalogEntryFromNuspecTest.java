/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.nuget.metadata;

import com.artipie.asto.test.TestResource;
import java.nio.charset.StandardCharsets;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link CatalogEntry.FromNuspec}.
 * @since 1.5
 */
class CatalogEntryFromNuspecTest {

    @Test
    void createsCatalogEntryForNewtonsoftJson() throws JSONException {
        JSONAssert.assertEquals(
            new CatalogEntry.FromNuspec(
                new Nuspec.Xml(
                    new TestResource("newtonsoft.json/12.0.3/newtonsoft.json.nuspec")
                        .asInputStream()
                )
            ).asJson().toString(),
            new String(
                new TestResource("CatalogEntryFromNuspecTest/newtonsoftjson.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            true
        );
    }

    @Test
    void createsCatalogEntryForSomePackage() throws JSONException {
        JSONAssert.assertEquals(
            new CatalogEntry.FromNuspec(
                new Nuspec.Xml(
                    new TestResource("CatalogEntryFromNuspecTest/some_package.nuspec")
                        .asInputStream()
                )
            ).asJson().toString(),
            new String(
                new TestResource("CatalogEntryFromNuspecTest/some_package.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            true
        );
    }
}

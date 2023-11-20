/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.meta;

import com.artipie.asto.test.TestResource;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.cactoos.set.SetOf;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link JsonMaid.Jackson}.
 * @since 0.1
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class JsonMaidTest {

    @Test
    void removesPackages() throws IOException, JSONException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final JsonFactory factory = new JsonFactory();
        final TestResource resource = new TestResource("repodata.json");
        new JsonMaid.Jackson(
            factory.createGenerator(stream).useDefaultPrettyPrinter(),
            factory.createParser(resource.asInputStream())
        ).clean(
            new SetOf<>(
                "b37f144a5c2349b1c58ef17a663cb79086a1f2f49e35503e4f411f6f698cee1a",
                "be2a62bd5a3a6abda7f2309f4f2ddce7bededb40adb91341f18438b246d7fc7e"
            )
        );
        JSONAssert.assertEquals(
            new String(
                new TestResource("JsonMaidTest/removesPackages.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            stream.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }

    @Test
    void removesLastPackage() throws IOException, JSONException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final JsonFactory factory = new JsonFactory();
        final TestResource resource = new TestResource("repodata.json");
        new JsonMaid.Jackson(
            factory.createGenerator(stream).useDefaultPrettyPrinter(),
            factory.createParser(resource.asInputStream())
        ).clean(new SetOf<>("47d6dd01a1cff52af31804bbfffb4341fd8676c75d00d120cc66d9709e78ea7f"));
        JSONAssert.assertEquals(
            new String(
                new TestResource("JsonMaidTest/removesLastPackage.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            stream.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }

    @Test
    void removesFirstPackage() throws IOException, JSONException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final JsonFactory factory = new JsonFactory();
        final TestResource resource = new TestResource("repodata.json");
        new JsonMaid.Jackson(
            factory.createGenerator(stream).useDefaultPrettyPrinter(),
            factory.createParser(resource.asInputStream())
        ).clean(new SetOf<>("4b36cb59651f6218449bd71a7d37182f062f545240b502eebed319f77fa54b08"));
        JSONAssert.assertEquals(
            new String(
                new TestResource("JsonMaidTest/removesFirstPackage.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            stream.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }

    @Test
    void removesAllTarPackages() throws IOException, JSONException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final JsonFactory factory = new JsonFactory();
        final TestResource resource = new TestResource("repodata.json");
        new JsonMaid.Jackson(
            factory.createGenerator(stream).useDefaultPrettyPrinter(),
            factory.createParser(resource.asInputStream())
        ).clean(
            new SetOf<>(
                "4b36cb59651f6218449bd71a7d37182f062f545240b502eebed319f77fa54b08",
                "b5f77880181b37fb2e180766869da6242648aaec5bdd6de89296d9dacd764c14",
                "b37f144a5c2349b1c58ef17a663cb79086a1f2f49e35503e4f411f6f698cee1a"
            )
        );
        JSONAssert.assertEquals(
            new String(
                new TestResource("JsonMaidTest/removesAllTarPackages.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            stream.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }

    @Test
    void removesAllCondaPackages() throws IOException, JSONException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final JsonFactory factory = new JsonFactory();
        final TestResource resource = new TestResource("repodata.json");
        new JsonMaid.Jackson(
            factory.createGenerator(stream).useDefaultPrettyPrinter(),
            factory.createParser(resource.asInputStream())
        ).clean(
            new SetOf<>(
                "be2a62bd5a3a6abda7f2309f4f2ddce7bededb40adb91341f18438b246d7fc7e",
                "47d6dd01a1cff52af31804bbfffb4341fd8676c75d00d120cc66d9709e78ea7f"
            )
        );
        JSONAssert.assertEquals(
            new String(
                new TestResource("JsonMaidTest/removesAllCondaPackages.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            stream.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }

    @Test
    void doesNothingIfPackageDoesNotExists() throws IOException, JSONException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final JsonFactory factory = new JsonFactory();
        final TestResource resource = new TestResource("repodata.json");
        new JsonMaid.Jackson(
            factory.createGenerator(stream).useDefaultPrettyPrinter(),
            factory.createParser(resource.asInputStream())
        ).clean(new SetOf<String>("098"));
        JSONAssert.assertEquals(
            new String(resource.asBytes(), StandardCharsets.UTF_8),
            stream.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }

    @Test
    void doesNothingIfChecksumsAreEmpty() throws IOException, JSONException {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final JsonFactory factory = new JsonFactory();
        final TestResource resource = new TestResource("repodata.json");
        new JsonMaid.Jackson(
            factory.createGenerator(stream).useDefaultPrettyPrinter(),
            factory.createParser(resource.asInputStream())
        ).clean(Collections.emptySet());
        JSONAssert.assertEquals(
            new String(resource.asBytes(), StandardCharsets.UTF_8),
            stream.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }

}

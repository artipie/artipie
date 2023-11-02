/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda.meta;

import com.artipie.asto.test.TestResource;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link MergedJson.Jackson}.
 * @since 0.2
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.UseObjectForClearerAPI"})
class MergedJsonTest {

    @Test
    void addsTarAndCondaPackages() throws IOException, JSONException {
        final ByteArrayOutputStream res = new ByteArrayOutputStream();
        final JsonFactory factory = new JsonFactory();
        new MergedJson.Jackson(
            factory.createGenerator(res).useDefaultPrettyPrinter(),
            Optional.empty()
        ).merge(
            new MapOf<String, JsonObject>(
                this.packageItem("decorator-4.2.1-py27_0.tar.bz2", "decorator-tar.json"),
                this.packageItem("notebook-6.1.1-py38_0.conda", "notebook-conda.json"),
                this.packageItem("pyqt-5.6.0-py36h0386399_5.tar.bz2", "pyqt-tar.json"),
                this.packageItem("tenacity-6.2.0-py37_0.conda", "tenacity-conda.json")
            )
        );
        JSONAssert.assertEquals(
            new String(
                new TestResource("MergedJsonTest/addsTarAndCondaPackages.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            res.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }

    @Test
    void addsTarPackages() throws IOException, JSONException {
        final ByteArrayOutputStream res = new ByteArrayOutputStream();
        final JsonFactory factory = new JsonFactory();
        new MergedJson.Jackson(
            factory.createGenerator(res).useDefaultPrettyPrinter(),
            Optional.empty()
        ).merge(
            new MapOf<String, JsonObject>(
                this.packageItem("decorator-4.2.1-py27_0.tar.bz2", "decorator-tar.json"),
                this.packageItem("pyqt-5.6.0-py36h0386399_5.tar.bz2", "pyqt-tar.json")
            )
        );
        JSONAssert.assertEquals(
            new String(
                new TestResource("MergedJsonTest/addsTarPackages.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            res.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }

    @Test
    void addsCondaPackages() throws IOException, JSONException {
        final ByteArrayOutputStream res = new ByteArrayOutputStream();
        final JsonFactory factory = new JsonFactory();
        new MergedJson.Jackson(
            factory.createGenerator(res).useDefaultPrettyPrinter(),
            Optional.empty()
        ).merge(
            new MapOf<String, JsonObject>(
                this.packageItem("notebook-6.1.1-py38_0.conda", "notebook-conda.json"),
                this.packageItem("tenacity-6.2.0-py37_0.conda", "tenacity-conda.json")
            )
        );
        JSONAssert.assertEquals(
            new String(
                new TestResource("MergedJsonTest/addsCondaPackages.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            res.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }

    @ParameterizedTest
    @CsvSource({
        "mp1_input.json,pyqt-5.6.0-py36h0386399_5.tar.bz2,pyqt-tar.json,mp1_output.json",
        "mp1_input.json,notebook-6.1.1-py38_0.conda,notebook-conda.json,mp2_output.json",
        "mp3_input.json,notebook-6.1.1-py38_0.conda,notebook-conda.json,mp3_output.json",
        "mp4_input.json,decorator-4.2.1-py27_0.tar.bz2,decorator-tar.json,mp4_output.json",
        "mp5_input.json,decorator-4.2.1-py27_0.tar.bz2,decorator-tar.json,mp3_output.json",
        "mp6_input.json,notebook-6.1.1-py38_0.conda,notebook-conda.json,mp3_output.json",
        "mp7_input.json,decorator-4.2.1-py27_0.tar.bz2,decorator-tar.json,mp7_output.json"
    })
    // @checkstyle ParameterNumberCheck (5 lines)
    void mergesPackage(final String input, final String pkg, final String file, final String out)
        throws IOException, JSONException {
        final ByteArrayOutputStream res = new ByteArrayOutputStream();
        final JsonFactory factory = new JsonFactory();
        try (InputStream stream =
            new TestResource(String.format("MergedJsonTest/%s", input)).asInputStream()) {
            new MergedJson.Jackson(
                factory.createGenerator(res).useDefaultPrettyPrinter(),
                Optional.of(
                    factory.createParser(
                        stream
                    )
                )
            ).merge(new MapOf<String, JsonObject>(this.packageItem(pkg, file)));
        }
        JSONAssert.assertEquals(
            new String(
                new TestResource(String.format("MergedJsonTest/%s", out)).asBytes(),
                StandardCharsets.UTF_8
            ),
            res.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }

    @ParameterizedTest
    @CsvSource({
        "mps1_input.json,mps1_output.json",
        "mps2_input.json,mps2_output.json",
        "mps3_input.json,mps3_output.json",
        "mps4_input.json,mps4_output.json",
        "mps5_input.json,mps5_output.json",
        "mps6_input.json,mps6_output.json",
        "mps7_input.json,mps6_output.json",
        "mps8_input.json,mps6_output.json"
    })
    void mergesPackages(final String input, final String out) throws IOException, JSONException {
        final ByteArrayOutputStream res = new ByteArrayOutputStream();
        final JsonFactory factory = new JsonFactory();
        try (InputStream stream =
            new TestResource(String.format("MergedJsonTest/%s", input)).asInputStream()) {
            new MergedJson.Jackson(
                factory.createGenerator(res).useDefaultPrettyPrinter(),
                Optional.of(
                    factory.createParser(
                        stream
                    )
                )
            ).merge(
                new MapOf<String, JsonObject>(
                    this.packageItem("decorator-4.2.1-py27_0.tar.bz2", "decorator-tar.json"),
                    this.packageItem("notebook-6.1.1-py38_0.conda", "notebook-conda.json"),
                    this.packageItem("pyqt-5.6.0-py36h0386399_5.tar.bz2", "pyqt-tar.json"),
                    this.packageItem("tenacity-6.2.0-py37_0.conda", "tenacity-conda.json")
                )
            );
        }
        JSONAssert.assertEquals(
            new String(
                new TestResource(String.format("MergedJsonTest/%s", out)).asBytes(),
                StandardCharsets.UTF_8
            ),
            res.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }

    private MapEntry<String, JsonObject> packageItem(final String filename, final String resourse) {
        return new MapEntry<String, JsonObject>(
            filename,
            Json.createReader(
                new TestResource(String.format("MergedJsonTest/%s", resourse)).asInputStream()
            ).readObject()
        );
    }

}

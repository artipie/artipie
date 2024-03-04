/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import javax.json.Json;
import javax.json.JsonObject;
import java.nio.charset.StandardCharsets;

/**
 * Test for {@link AstoMergedJson}.
 */
class AstoMergedJsonTest {

    /**
     * Test key.
     */
    private static final Key.From KEY = new Key.From("repodata.json");

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void addsItemsWhenInputIsPresent() throws JSONException {
        new TestResource("MergedJsonTest/mp1_input.json")
            .saveTo(this.asto, AstoMergedJsonTest.KEY);
        new AstoMergedJson(this.asto, AstoMergedJsonTest.KEY).merge(
            new MapOf<>(
                this.packageItem("notebook-6.1.1-py38_0.conda", "notebook-conda.json"),
                this.packageItem("pyqt-5.6.0-py36h0386399_5.tar.bz2", "pyqt-tar.json")
            )
        ).toCompletableFuture().join();
        JSONAssert.assertEquals(
            this.getRepodata(),
            new String(
                new TestResource("AstoMergedJsonTest/addsItemsWhenInputIsPresent.json")
                    .asBytes(),
                StandardCharsets.UTF_8
            ),
            true
        );
    }

    @Test
    void addsItemsWhenInputIsAbsent() throws JSONException {
        new AstoMergedJson(this.asto, AstoMergedJsonTest.KEY).merge(
            new MapOf<>(
                this.packageItem("notebook-6.1.1-py38_0.conda", "notebook-conda.json"),
                this.packageItem("pyqt-5.6.0-py36h0386399_5.tar.bz2", "pyqt-tar.json")
            )
        ).toCompletableFuture().join();
        JSONAssert.assertEquals(
            this.getRepodata(),
            new TestResource("AstoMergedJsonTest/addsItemsWhenInputIsAbsent.json")
                .asString(),
            true
        );
    }

    private String getRepodata() {
        return this.asto.value(AstoMergedJsonTest.KEY).join().asString();
    }

    private MapEntry<String, JsonObject> packageItem(final String filename,
        final String resourse) {
        return new MapEntry<>(
            filename,
            Json.createReader(
                new TestResource(String.format("MergedJsonTest/%s", resourse)).asInputStream()
            ).readObject()
        );
    }

}

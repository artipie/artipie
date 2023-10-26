/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import javax.json.Json;
import javax.json.JsonObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link MetaUpdate.ByJson}.
 * @since 0.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class MetaUpdateByJsonTest {
    /**
     * Storage.
     */
    private Storage asto;

    @BeforeEach
    void setUp() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void createsMetaFileWhenItNotExist() {
        final Key prefix = new Key.From("prefix");
        new MetaUpdate.ByJson(this.cliMeta())
            .update(new Key.From(prefix), this.asto)
            .join();
        MatcherAssert.assertThat(
            this.asto.exists(new Key.From(prefix, "meta.json")).join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void updatesExistedMetaFile() {
        final Key prefix = new Key.From("prefix");
        new TestResource("json/simple-project-1.0.2.json")
            .saveTo(this.asto, new Key.From(prefix, "meta.json"));
        new MetaUpdate.ByJson(this.cliMeta())
            .update(new Key.From(prefix), this.asto)
            .join();
        MatcherAssert.assertThat(
            new JsonFromMeta(this.asto, prefix).json()
                .getJsonObject("versions")
                .keySet(),
            Matchers.containsInAnyOrder("1.0.1", "1.0.2")
        );
    }

    private JsonObject cliMeta() {
        return Json.createReader(
            new TestResource("json/cli_publish.json").asInputStream()
        ).readObject();
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.jfr;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.test.TestStoragesCache;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingStream;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests to check the JFR storage create event.
 *
 * @since 0.28.0
 * @checkstyle MagicNumberCheck (500 lines)
 */
public class StorageCreateEventTest {

    @Test
    void shouldPublishStorageCreateEventWhenCreate() {
        try (RecordingStream rs = new RecordingStream()) {
            final AtomicReference<RecordedEvent> ref = new AtomicReference<>();
            rs.onEvent("artipie.StorageCreate", ref::set);
            rs.startAsync();
            new TestStoragesCache().storage(
                Yaml.createYamlMappingBuilder()
                    .add("type", "fs")
                    .add("path", "")
                    .build()
            );
            Awaitility.waitAtMost(3_000, TimeUnit.MILLISECONDS)
                .until(() -> ref.get() != null);
            final RecordedEvent event = ref.get();
            MatcherAssert.assertThat(
                event.getString("storage"),
                Is.is("FS: ")
            );
            Assertions.assertTrue(event.getDuration().toNanos() > 0);
        }
    }
}

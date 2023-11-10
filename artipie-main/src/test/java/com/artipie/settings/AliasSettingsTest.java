/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.test.TestStoragesCache;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link AliasSettings}.
 * @since 0.28
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AliasSettingsTest {

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @ParameterizedTest
    @ValueSource(strings =
        {"alice/my-maven/_storages.yaml", "alice/_storages.yaml", "_storages.yaml"}
    )
    void findsRepoAliases(final String aliases) {
        this.asto.save(
            new Key.From(aliases),
            new Content.From(
                String.join(
                    "\n",
                    "storages:",
                    "  def:",
                    "    type: fs",
                    "    path: any"
                ).getBytes(StandardCharsets.UTF_8)
            )
        ).join();
        MatcherAssert.assertThat(
            new AliasSettings(this.asto).find(new Key.From("alice/my-maven")).join()
                .storage(new TestStoragesCache(), "def").identifier(),
            new IsEqual<>("FS: any")
        );
    }

    @ParameterizedTest
    @ValueSource(strings =
        {"alice/my-maven/_storages.yaml", "alice/_storages.yaml"}
    )
    void throwsErrorIfNotFound(final String aliases) {
        this.asto.save(
            new Key.From(aliases),
            new Content.From(
                String.join(
                    "\n",
                    "storages:",
                    "  def:",
                    "    type: fs",
                    "    path: any"
                ).getBytes(StandardCharsets.UTF_8)
            )
        ).join();
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new AliasSettings(this.asto).find(new Key.From("john/my-deb")).join()
                .storage(new TestStoragesCache(), "def")
        );
    }

}

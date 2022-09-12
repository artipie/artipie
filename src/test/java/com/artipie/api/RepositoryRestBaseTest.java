/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import io.vertx.junit5.VertxExtension;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link RepositoryRest} with `flat` layout.
 * @since 0.26
 */
@ExtendWith(VertxExtension.class)
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public abstract class RepositoryRestBaseTest extends RestApiServerBase {
    /**
     * Temp dir.
     * @checkstyle VisibilityModifierCheck (500 lines)
     */
    @TempDir
    Path temp;

    /**
     * Test data storage.
     */
    BlockingStorage data;

    /**
     * Before each method creates test data storage instance.
     */
    @BeforeEach
    void init() {
        this.data = new BlockingStorage(new FileStorage(this.temp));
    }

    /**
     * Provides repository settings for data storage.
     * @return Data storage settings
     */
    String repoSettings() {
        return String.join(
            System.lineSeparator(),
            "repo:",
            "  type: binary",
            "  storage:",
            "    type: fs",
            String.format("    path: %s", this.temp.toString())
        );
    }
}

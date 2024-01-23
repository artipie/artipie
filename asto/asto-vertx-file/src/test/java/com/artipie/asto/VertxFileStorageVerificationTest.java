/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.fs.VertxFileStorage;
import com.artipie.asto.test.StorageWhiteboxVerification;
import io.vertx.reactivex.core.Vertx;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.io.TempDir;

/**
 * Vertx file storage verification test.
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class VertxFileStorageVerificationTest extends StorageWhiteboxVerification {

    /**
     * Vert.x file System.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Temp dir.
     */
    @TempDir
    private Path temp;

    @Override
    protected Storage newStorage() throws Exception {
        return new VertxFileStorage(
            this.temp.resolve("base"),
            VertxFileStorageVerificationTest.VERTX
        );
    }

    @Override
    protected Optional<Storage> newBaseForRootSubStorage() {
        return Optional.of(
            new VertxFileStorage(
                this.temp.resolve("root-sub-storage"), VertxFileStorageVerificationTest.VERTX
            )
        );
    }

    @Override
    protected Optional<Storage> newBaseForSubStorage() {
        return Optional.of(
            new VertxFileStorage(
                this.temp.resolve("sub-storage"), VertxFileStorageVerificationTest.VERTX
            )
        );
    }

    @AfterAll
    static void tearDown() throws Exception {
        VertxFileStorageVerificationTest.VERTX.close();
    }

}

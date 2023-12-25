/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.StorageWhiteboxVerification;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.io.TempDir;

/**
 * File storage verification test.
 *
 * @checkstyle ProtectedMethodInFinalClassCheck (500 lines)
 * @since 1.14.0
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class FileStorageWhiteboxVerificationTest extends StorageWhiteboxVerification {

    /**
     * Temp test dir.
     */
    @TempDir
    private Path temp;

    @Override
    protected Storage newStorage() {
        return new FileStorage(this.temp.resolve("base"));
    }

    @Override
    protected Optional<Storage> newBaseForRootSubStorage() {
        return Optional.of(new FileStorage(this.temp.resolve("root-sub-storage")));
    }

    @Override
    protected Optional<Storage> newBaseForSubStorage() throws Exception {
        return Optional.of(new FileStorage(this.temp.resolve("sub-storage")));
    }
}

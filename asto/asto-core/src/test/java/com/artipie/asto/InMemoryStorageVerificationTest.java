/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.StorageWhiteboxVerification;

/**
 * In memory storage verification test.
 *
 * @since 1.14.0
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public final class InMemoryStorageVerificationTest extends StorageWhiteboxVerification {

    @Override
    protected Storage newStorage() throws Exception {
        return new InMemoryStorage();
    }
}

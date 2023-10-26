/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package  com.artipie.conan;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import io.reactivex.Completable;

/**
 * Conan repo frontend.
 * @since 0.1
 */
public final class ConanRepo {

    /**
     * Primary storage.
     */
    @SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
    private final Storage storage;

    /**
     * Main constructor.
     * @param storage Asto storage object
     */
    public ConanRepo(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Updates repository incrementally.
     * @param prefix Repo prefix
     * @return Completable action
     * @checkstyle NonStaticMethodCheck (5 lines)
     */
    public Completable batchUpdateIncrementally(final Key prefix) {
        return null;
    }
}

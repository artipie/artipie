/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Single;
import java.util.concurrent.CompletionStage;

/**
 * Artipie repositories created from {@link Settings}.
 *
 * @since 0.13
 * @todo #597:30min Add unit tests for RepositoriesFromStorage class.
 *  `RepositoriesFromStorage` class was extracted from existing code and lacks test coverage.
 *  It's methods should be tested for all important execution paths.
 */
public final class RepositoriesFromStorage implements Repositories {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     *
     * @param storage Storage.
     */
    public RepositoriesFromStorage(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletionStage<RepoConfig> config(final String name) {
        return Single.zip(
            SingleInterop.fromFuture(
                this.storage.value(new Key.From(String.format("%s.yaml", name)))
            ),
            SingleInterop.fromFuture(StorageAliases.find(this.storage, new Key.From(name))),
            (data, aliases) -> SingleInterop.fromFuture(
                RepoConfig.fromPublisher(aliases, new Key.From(name), data)
            )
        ).flatMap(self -> self).to(SingleInterop.get());
    }
}

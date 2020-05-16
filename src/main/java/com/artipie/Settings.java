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

import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.http.auth.Authentication;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletionStage;

/**
 * Application settings.
 *
 * @since 0.1
 */
public interface Settings {
    /**
     * Provides a storage.
     *
     * @return Storage instance.
     * @throws IOException In case of problems with reading settings.
     */
    Storage storage() throws IOException;

    /**
     * Provides authorization.
     *
     * @return Authentication instance
     * @throws IOException On Error
     */
    CompletionStage<Authentication> auth() throws IOException;

    /**
     * Fake {@link Settings} using a file storage.
     *
     * @since 0.2
     */
    final class Fake implements Settings {

        /**
         * Storage path.
         */
        private final Path storage;

        /**
         * Vertx.
         */
        private final Vertx vertx;

        /**
         * Ctor.
         *
         * @param storage Storage path
         * @param vertx Vertx
         */
        public Fake(final Path storage, final Vertx vertx) {
            this.storage = storage;
            this.vertx = vertx;
        }

        @Override
        public Storage storage() throws IOException {
            return new FileStorage(this.storage, this.vertx.fileSystem());
        }

        @Override
        public CompletionStage<Authentication> auth() throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}

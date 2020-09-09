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

package com.artipie.api.artifactory;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.common.RsJson;
import java.nio.ByteBuffer;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import org.reactivestreams.Publisher;

/**
 * Get storage slice. See https://github.com/artipie/artipie/issues/545
 *
 * @since 0.10
 */
public final class GetStorageSlice implements Slice {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * New storage list slice.
     * @param storage Repository storage
     */
    public GetStorageSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            this.storage.list(Key.ROOT)
                .thenApply(
                    list -> {
                        final KeyList keys = new KeyList();
                        list.forEach(keys::add);
                        return keys.print(new JsonOutput());
                    }
                ).thenApply(RsJson::new)
        );
    }

    /**
     * JSON array output for key list.
     * @since 0.10
     */
    private static final class JsonOutput implements KeyList.KeysFormat<JsonArray> {

        /**
         * Array builder.
         */
        private final JsonArrayBuilder builder;

        /**
         * New JSON key list output.
         */
        JsonOutput() {
            this(Json.createArrayBuilder());
        }

        /**
         * New JSON key list output.
         * @param builder Array builder
         */
        private JsonOutput(final JsonArrayBuilder builder) {
            this.builder = builder;
        }

        @Override
        public void add(final Key item, final boolean parent) {
            this.builder.add(
                Json.createObjectBuilder()
                    .add("uri", String.format("/%s", item.string()))
                    .add("folder", Boolean.toString(parent))
            );
        }

        @Override
        public JsonArray result() {
            return this.builder.build();
        }
    }
}

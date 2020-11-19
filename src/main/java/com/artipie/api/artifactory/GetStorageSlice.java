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

import com.artipie.RepoConfig;
import com.artipie.RepositoriesFromStorage;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.common.RsJson;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import org.reactivestreams.Publisher;

/**
 * Get storage slice. See https://github.com/artipie/artipie/issues/545
 *
 * @since 0.10
 */
public final class GetStorageSlice implements Slice {

    /**
     * Artipie settings storage.
     */
    private final Storage storage;

    /**
     * Artipie path pattern.
     */
    private final Pattern path;

    /**
     * New storage list slice.
     * @param storage Artipie settings storage
     * @param path Artipie path pattern
     */
    public GetStorageSlice(final Storage storage, final Pattern path) {
        this.storage = storage;
        this.path = path;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Request request = new Request(this.path, line);
        final Key root = request.root();
        return new AsyncResponse(
            this.repoStorage(request.repo()).thenCompose(
                repo -> repo.list(root).thenApply(
                    list -> {
                        final KeyList keys = new KeyList(root);
                        list.forEach(keys::add);
                        return keys.print(new JsonOutput());
                    }
                ).thenApply(RsJson::new)
            )
        );
    }

    /**
     * Get storage by repo name.
     *
     * @param name Repo name.
     * @return Repo storage.
     */
    private CompletionStage<Storage> repoStorage(final String name) {
        return new RepositoriesFromStorage(this.storage).config(name)
            .thenApply(RepoConfig::storage);
    }

    /**
     * JSON array output for key list.
     * @since 0.10
     */
    private static final class JsonOutput implements KeyList.KeysFormat<JsonObject> {

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
        public JsonObject result() {
            return Json.createObjectBuilder().add("files", this.builder).build();
        }
    }

    /**
     * Request to GetStorageSlice.
     *
     * @since 0.11
     */
    public static final class Request {

        /**
         * RegEx pattern for path.
         */
        public static final Pattern PATH = Pattern.compile("/api/storage(?<target>/.+)");

        /**
         * Artipie path pattern.
         */
        private final Pattern path;

        /**
         * Request line.
         */
        private final String line;

        /**
         * Ctor.
         *
         * @param path Path pattern.
         * @param line Request line.
         */
        public Request(final Pattern path, final String line) {
            this.path = path;
            this.line = line;
        }

        /**
         * Read repo name for files listing.
         *
         * @return Repo name.
         */
        public String repo() {
            final String target = this.target();
            final Key root = this.root();
            return target.substring(1, target.length() - root.string().length() - 1);
        }

        /**
         * Root key for files listing.
         *
         * @return Root key.
         */
        public Key root() {
            final String target = this.target();
            final Matcher matcher = this.path.matcher(target);
            if (matcher.matches()) {
                return new Key.From(matcher.group(1).substring(1));
            } else {
                throw new IllegalArgumentException(
                    String.format("Cannot find repo in path: '%s'", target)
                );
            }
        }

        /**
         * Reads target part from path.
         *
         * @return Target.
         */
        private String target() {
            final String rqpath = new RequestLineFrom(this.line).uri().getPath();
            final Matcher matcher = PATH.matcher(rqpath);
            if (matcher.matches()) {
                return matcher.group("target");
            } else {
                throw new IllegalArgumentException(String.format("Invalid path: '%s'", rqpath));
            }
        }
    }
}

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
package com.artipie.api;

import com.amihaiemil.eoyaml.Scalar;
import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.Settings;
import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Create repo API.
 * @since 0.6
 */
final class ApiRepoCreateSlice implements Slice {

    /**
     * URI path pattern.
     */
    private static final Pattern PTN = Pattern.compile("/api/repos/(?<key>[^/.]+/[^/.]+)");

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * New create API.
     * @param settings Artipie settings
     */
    ApiRepoCreateSlice(final Settings settings) {
        this.settings = settings;
    }

    @Override
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers, final Publisher<ByteBuffer> body) {
        final Matcher matcher = PTN.matcher(new RequestLineFrom(line).uri().getPath());
        if (!matcher.matches()) {
            throw new IllegalStateException("Should match");
        }
        final String name = matcher.group("key");
        final Key.From key = new Key.From(String.format("%s.yaml", name));
        // @checkstyle LineLengthCheck (50 lines)
        // @checkstyle ReturnCountCheck (50 lines)
        return new AsyncResponse(
            Single.fromCallable(this.settings::storage).map(RxStorageWrapper::new).flatMap(
                storage -> storage.exists(key).flatMap(
                    exist -> {
                        if (exist) {
                            return Single.just(new RsWithStatus(RsStatus.CONFLICT));
                        } else {
                            return storage.save(
                                key,
                                new Content.From(
                                    new Concatenation(body).single().map(buf -> new Remaining(buf).bytes())
                                        .map(bytes -> Yaml.createYamlInput(new String(bytes, StandardCharsets.UTF_8)).readYamlMapping())
                                        .map(
                                            yaml -> {
                                                final YamlMapping repo = yaml.yamlMapping("repo");
                                                final YamlNode type = repo.value("type");
                                                if (type == null || !Scalar.class.isAssignableFrom(type.getClass())) {
                                                    throw new IllegalStateException("Repository type required");
                                                }
                                                final YamlNode stor = repo.value("storage");
                                                if (stor == null || !Scalar.class.isAssignableFrom(stor.getClass())) {
                                                    throw new IllegalStateException("Repository storage is required");
                                                }
                                                return Yaml.createYamlMappingBuilder().add(
                                                    "repo",
                                                    Yaml.createYamlMappingBuilder()
                                                        .add("type", type)
                                                        .add("storage", stor)
                                                        .add("permissions", repo.value("permissions"))
                                                        .build()
                                                ).build()
                                                    .toString()
                                                    .getBytes(StandardCharsets.UTF_8);
                                            }
                                        ).flatMapPublisher(bytes -> Flowable.just(ByteBuffer.wrap(bytes)))
                                )
                            ).andThen(Single.just(new RsWithStatus(RsStatus.OK)));
                        }
                    }
                )
            ).to(SingleInterop.get())
        );
    }
}

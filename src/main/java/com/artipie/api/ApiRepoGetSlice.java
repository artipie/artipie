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
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.artipie.asto.Concatenation;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Repo {@code GET} API.
 * @since 0.6
 */
final class ApiRepoGetSlice implements Slice {

    /**
     * URI path pattern.
     */
    private static final Pattern PTN = Pattern.compile("/api/repos/(?<key>[^/.]+/[^/.]+)");

    /**
     * Artipie settings storage.
     */
    private final Storage storage;

    /**
     * New repo API.
     * @param storage Artipie settings storage
     */
    ApiRepoGetSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Matcher matcher = PTN.matcher(new RequestLineFrom(line).uri().getPath());
        if (!matcher.matches()) {
            throw new IllegalStateException("Should match");
        }
        final String name = matcher.group("key");
        final Key.From key = new Key.From(String.format("%s.yaml", name));
        // @checkstyle LineLengthCheck (50 lines)
        return new AsyncResponse(
            Single.fromCallable(() -> this.storage).map(RxStorageWrapper::new).flatMap(
                rxstorage -> rxstorage.exists(key).filter(exists -> exists)
                    .flatMapSingleElement(
                        ignore -> rxstorage.value(key)
                            .flatMap(pub -> new Concatenation(pub).single())
                            .map(
                                data -> Yaml.createYamlInput(
                                    new String(new Remaining(data).bytes(), StandardCharsets.UTF_8)
                                ).readYamlMapping()
                            ).map(
                                config -> {
                                    final YamlMapping repo = config.yamlMapping("repo");
                                    YamlMappingBuilder builder = Yaml.createYamlMappingBuilder();
                                    builder = builder.add("type", repo.value("type"));
                                    if (repo.value("storage") != null
                                        && Scalar.class.isAssignableFrom(repo.value("storage").getClass())) {
                                        builder = builder.add("storage", repo.value("storage"));
                                    }
                                    builder = builder.add("permissions", repo.value("permissions"));
                                    return Yaml.createYamlMappingBuilder().add("repo", builder.build()).build();
                                }
                            ).<Response>map(RsYaml::new)
                    ).switchIfEmpty(Single.just(new RsWithStatus(RsStatus.NOT_FOUND)))
            ).to(SingleInterop.get())
        );
    }
}

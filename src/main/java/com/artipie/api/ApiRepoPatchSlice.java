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
import com.amihaiemil.eoyaml.YamlMappingBuilder;
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
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Patch repo API.
 * @since 0.6
 */
final class ApiRepoPatchSlice implements Slice {

    /**
     * URI path pattern.
     */
    private static final Pattern PTN = Pattern.compile("/api/repos/(?<key>[^/.]+/[^/.]+)");

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * New patch API.
     * @param settings Artipie settings
     */
    ApiRepoPatchSlice(final Settings settings) {
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
        return new AsyncResponse(
            Single.zip(
                new Concatenation(body).single().map(buf -> new Remaining(buf).bytes())
                    .map(bytes -> Yaml.createYamlInput(new String(bytes, StandardCharsets.UTF_8)).readYamlMapping()),
                Single.fromCallable(this.settings::storage).map(RxStorageWrapper::new)
                    .flatMap(storage -> storage.value(key))
                    .flatMap(pub -> new Concatenation(pub).single())
                    .map(data -> Yaml.createYamlInput(new String(new Remaining(data).bytes(), StandardCharsets.UTF_8)).readYamlMapping()),
                (patch, source) -> {
                    patch = patch.yamlMapping("repo");
                    YamlMappingBuilder repo = Yaml.createYamlMappingBuilder();
                    repo = repo.add("type", source.yamlMapping("repo").value("type"));
                    if (patch.value("type") != null) {
                        repo = repo.add("type", patch.value("type"));
                    }
                    repo = repo.add("storage", source.yamlMapping("repo").value("storage"));
                    if (patch.value("storage") != null && Scalar.class.isAssignableFrom(patch.value("storage").getClass())) {
                        repo = repo.add("storage", patch.value("storage"));
                    }
                    repo = repo.add("permissions", source.yamlMapping("repo").value("permissions"));
                    return Yaml.createYamlMappingBuilder()
                        .add("repo", repo.build())
                        .build();
                }
            ).map(
                yaml -> new Content.From(yaml.toString().getBytes(StandardCharsets.UTF_8))
            ).flatMapCompletable(
                content -> Single.fromCallable(this.settings::storage)
                    .map(RxStorageWrapper::new).flatMapCompletable(
                        rxsto -> rxsto.save(key, content)
                    )
            ).toSingle(() -> new RsWithStatus(RsStatus.OK))
        );
    }
}

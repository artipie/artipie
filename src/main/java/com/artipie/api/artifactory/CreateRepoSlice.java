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

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.api.ContentAs;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.JsonObject;
import org.reactivestreams.Publisher;

/**
 * Artifactory create repo API slice, it accepts json and create new docker repository by
 * creating corresponding YAML configuration.
 * @since 0.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class CreateRepoSlice implements Slice {

    /**
     * URI path pattern.
     */
    private static final Pattern PTN =
        Pattern.compile("/api/repositories/(?<first>[^/.]+)(?<second>/[^/.]+)?/?");

    /**
     * Artipie settings storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Artipie settings storage
     */
    public CreateRepoSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        // @checkstyle ReturnCountCheck (20 lines)
        return new AsyncResponse(
            Single.just(body).to(ContentAs.JSON).flatMap(
                json -> Single.fromFuture(
                    valid(json).map(
                        name -> {
                            final Key key = CreateRepoSlice.yamlKey(line, name);
                            return this.storage.exists(key)
                                .thenCompose(
                                    exists -> {
                                        final CompletionStage<Response> res;
                                        if (exists) {
                                            res = CompletableFuture.completedStage(
                                                new RsWithStatus(RsStatus.BAD_REQUEST)
                                            );
                                        } else {
                                            res = this.storage.save(
                                                key,
                                                new Content.From(
                                                    CreateRepoSlice.yaml().toString()
                                                        .getBytes(StandardCharsets.UTF_8)
                                                )
                                            ).thenApply(ignored -> new RsWithStatus(RsStatus.OK));
                                        }
                                        return res;
                                    }
                                );
                        }
                    ).orElse(
                        CompletableFuture.completedFuture(new RsWithStatus(RsStatus.BAD_REQUEST))
                    )
                )
            )
        );
    }

    /**
     * Checks if json is valid (contains new repo key and supported setting) and
     * return new repo name.
     * @param json Json to read repo name from
     * @return True if json is correct
     */
    private static Optional<String> valid(final JsonObject json) {
        final Optional<String> res;
        final String key = json.getString("key", "");
        if (!key.isEmpty() && "local".equals(json.getString("rclass", ""))
            && "docker".equals(json.getString("packageType", ""))
            && "V2".equals(json.getString("dockerApiVersion", ""))) {
            res = Optional.of(key);
        } else {
            res = Optional.empty();
        }
        return res;
    }

    /**
     * User from request line.
     * @param line Line
     * @param repo Repo name
     * @return Username if present
     */
    private static Key yamlKey(final String line, final String repo) {
        final Matcher matcher = PTN.matcher(new RequestLineFrom(line).uri().getPath());
        if (!matcher.matches()) {
            throw new UnsupportedOperationException("Unsupported request");
        }
        return new Key.From(
            String.format(
                "%s%s.yaml",
                Optional.ofNullable(
                    matcher.group("second")
                ).map(present -> String.format("%s/", matcher.group("first"))).orElse(""),
                repo
            )
        );
    }

    /**
     * Creates yaml mapping for the new repository.
     * @return Yaml configuration
     */
    private static YamlMapping yaml() {
        return Yaml.createYamlMappingBuilder().add(
            "repo",
            Yaml.createYamlMappingBuilder()
                .add("type", "docker")
                .add("storage", "default")
                .add(
                    "permissions",
                    Yaml.createYamlMappingBuilder()
                        .add("*", Yaml.createYamlSequenceBuilder().add("*").build())
                        .build()
                ).build()
        ).build();
    }

}

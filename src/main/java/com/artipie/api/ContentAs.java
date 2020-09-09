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

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Concatenation;
import com.artipie.asto.Remaining;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.reactivestreams.Publisher;

/**
 * Rx publisher transformer to single.
 * @param <T> Single type
 * @since 0.8
 */
public final class ContentAs<T>
    implements Function<Single<? extends Publisher<ByteBuffer>>, Single<? extends T>> {

    /**
     * Content as string.
     */
    public static final ContentAs<String> STRING = new ContentAs<>(
        bytes -> new String(bytes, StandardCharsets.UTF_8)
    );

    /**
     * Content as {@code long} number.
     */
    public static final ContentAs<Long> LONG = new ContentAs<>(
        bytes -> Long.valueOf(new String(bytes, StandardCharsets.US_ASCII))
    );

    /**
     * Content as YAML mapping.
     */
    public static final ContentAs<YamlMapping> YAML = new ContentAs<>(
        bytes -> Yaml.createYamlInput(new String(bytes, StandardCharsets.UTF_8))
            .readYamlMapping()
    );

    /**
     * Content as JSON.
     */
    public static final ContentAs<JsonObject> JSON = new ContentAs<>(
        bytes -> {
            try (JsonReader reader = Json.createReader(new ByteArrayInputStream(bytes))) {
                return reader.readObject();
            }
        }
    );

    /**
     * Transform function.
     */
    private final Function<byte[], T> transform;

    /**
     * Ctor.
     * @param transform Transform function
     */
    private ContentAs(final Function<byte[], T> transform) {
        this.transform = transform;
    }

    @Override
    public Single<? extends T> apply(
        final Single<? extends Publisher<ByteBuffer>> content
    ) {
        return content.flatMap(pub -> new Concatenation(pub).single())
            .map(Remaining::new)
            .map(Remaining::bytes)
            .map(this.transform);
    }
}

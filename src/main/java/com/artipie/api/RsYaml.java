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

import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.rs.Header;
import com.artipie.http.rs.RsStatus;
import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Response with Yaml document.
 * @since 0.6
 */
public final class RsYaml implements Response {

    /**
     * Yaml supplier.
     */
    private final Supplier<? extends YamlNode> yaml;

    /**
     * Charset encoding.
     */
    private final Charset encoding;

    /**
     * Response from Yaml document.
     * @param yaml Yaml document
     */
    public RsYaml(final YamlNode yaml) {
        this(() -> yaml);
    }

    /**
     * Response from Yaml supplier.
     * @param yaml Yaml document supplier
     */
    public RsYaml(final Supplier<? extends YamlNode> yaml) {
        this(yaml, StandardCharsets.UTF_8);
    }

    /**
     * Response from Yaml supplier with charset encoding.
     * @param yaml Yaml document supplier
     * @param encoding Charset encoding
     */
    public RsYaml(final Supplier<? extends YamlNode> yaml, final Charset encoding) {
        this.yaml = yaml;
        this.encoding = encoding;
    }

    @Override
    public CompletionStage<Void> send(final Connection connection) {
        final byte[] bytes = this.yaml.get().toString().getBytes(this.encoding);
        return connection.accept(
            RsStatus.OK,
            new Headers.From(
                new Header(
                    "content-type",
                    String.format("text/yaml; charset=%s", this.encoding.displayName())
                ),
                new Header("content-length", Integer.toString(bytes.length))
            ),
            Flowable.just(ByteBuffer.wrap(bytes))
        );
    }
}

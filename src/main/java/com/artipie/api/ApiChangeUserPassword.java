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
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.Settings;
import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Remaining;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.Header;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.KeyFromPath;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.reactivestreams.Publisher;

/**
 * Change user password slice.
 * @since 0.6
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class ApiChangeUserPassword implements Slice {

    /**
     * URI path pattern.
     */
    private static final Pattern PTN = Pattern.compile("/api/users/(?<user>[^/.]+)/password");

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * New create API.
     * @param settings Artipie settings
     */
    ApiChangeUserPassword(final Settings settings) {
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
        final String user = matcher.group("user");
        // @checkstyle LineLengthCheck (100 lines)
        return new AsyncResponse(
            Single.fromCallable(() -> this.settings.meta().yamlMapping("credentials"))
                .map(cred -> new KeyFromPath(cred.string("path")))
                .flatMapCompletable(
                    key ->
                        Single.zip(
                            new RxStorageWrapper(this.settings.storage())
                                .value(key)
                                .flatMap(pub -> new Concatenation(pub).single())
                                .map(
                                    buf -> Yaml.createYamlInput(
                                        new String(new Remaining(buf).bytes(), StandardCharsets.UTF_8)
                                    ).readYamlMapping()),
                            new Concatenation(body).single()
                                .map(buf -> new String(new Remaining(buf).bytes(), StandardCharsets.UTF_8))
                                .map(
                                    encoded -> URLEncodedUtils.parse(encoded, StandardCharsets.UTF_8)
                                        .stream()
                                        .filter(pair -> pair.getName().equals("password"))
                                        .map(NameValuePair::getValue)
                                        .findFirst().orElseThrow()
                                ),
                            (YamlMapping yaml, String pass) -> {
                                YamlMappingBuilder result = Yaml.createYamlMappingBuilder();
                                final YamlMapping credentials = yaml.yamlMapping("credentials");
                                final List<YamlNode> keep = credentials.keys().stream()
                                    .filter(node -> !node.asScalar().value().equals(user)).collect(Collectors.toList());
                                for (final YamlNode node : keep) {
                                    result = result.add(node, credentials.value(node));
                                }
                                result = result.add(
                                    user,
                                    Yaml.createYamlMappingBuilder()
                                        .add("pass", String.format("sha256:%s", DigestUtils.sha256Hex(pass)))
                                        .build()
                                );
                                return Yaml.createYamlMappingBuilder()
                                    .add("credentials", result.build()).build()
                                    .toString();
                            }
                        ).flatMapCompletable(
                            yaml -> new RxStorageWrapper(this.settings.storage())
                                .save(key, new Content.From(yaml.getBytes(StandardCharsets.UTF_8))))
                )
                .toSingleDefault(
                    new RsWithHeaders(
                        new RsWithStatus(RsStatus.FOUND),
                        new Header("Location", String.format("/%s", user))
                    )
                ).to(SingleInterop.get())
        );
    }
}

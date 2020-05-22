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

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.auth.AuthFromEnv;
import com.artipie.auth.AuthFromYaml;
import com.artipie.http.auth.Authentication;
import com.artipie.http.slice.KeyFromPath;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.reactivestreams.Publisher;

/**
 * Settings built from YAML.
 *
 * @since 0.1
 */
public final class YamlSettings implements Settings {

    /**
     * Meta section.
     */
    private static final String META = "meta";

    /**
     * YAML file content.
     */
    private final String content;

    /**
     * The Vert.x instance.
     */
    private final Vertx vertx;

    /**
     * Ctor.
     * @param content YAML file content.
     * @param vertx The Vert.x instance.
     */
    public YamlSettings(final String content, final Vertx vertx) {
        this.content = content;
        this.vertx = vertx;
    }

    @Override
    public Storage storage() throws IOException {
        return new YamlStorageSettings(
            Yaml.createYamlInput(this.content)
                .readYamlMapping()
                .yamlMapping(YamlSettings.META)
                .yamlMapping("storage"),
                this.vertx
        ).storage();
    }

    @Override
    public CompletionStage<Authentication> auth() throws IOException {
        final YamlMapping cred = Yaml.createYamlInput(this.content)
            .readYamlMapping()
            .yamlMapping(YamlSettings.META)
            .yamlMapping("credentials");
        final CompletionStage<Authentication> res;
        final String path = "path";
        if (cred != null && "file".equals(cred.string("type")) && cred.string(path) != null) {
            final KeyFromPath key = new KeyFromPath(cred.string(path));
            final Storage strg = this.storage();
            res = strg.exists(key).thenCompose(
                exists -> {
                    final CompletionStage<Authentication> auth;
                    if (exists) {
                        auth = strg.value(key).thenCompose(
                            file -> yamlFromPublisher(file).thenApply(AuthFromYaml::new)
                        );
                    } else {
                        auth = CompletableFuture.completedStage(new AuthFromEnv());
                    }
                    return auth;
                }
            );
        } else {
            res = CompletableFuture.completedStage(new AuthFromEnv());
        }
        return res;
    }

    /**
     * Create async yaml config from content publisher.
     * @param pub Flow publisher
     * @return Completion stage of yaml
     * @todo #146:30min Extract this method to a class: we have the same method in RepoConfig. After
     *  extracting use this new class here and in RepoConfig. Do not forget about test.
     */
    private static CompletionStage<YamlMapping> yamlFromPublisher(
        final Publisher<ByteBuffer> pub
    ) {
        return Flowable.fromPublisher(pub)
            .reduce(
                new StringBuilder(),
                (acc, buf) -> acc.append(
                    new String(new Remaining(buf).bytes(), StandardCharsets.UTF_8)
                )
            )
            .doOnSuccess(yaml -> Logger.debug(RepoConfig.class, "parsed yaml config: %s", yaml))
            .map(content -> Yaml.createYamlInput(content.toString()).readYamlMapping())
            .to(SingleInterop.get())
            .toCompletableFuture();
    }
}

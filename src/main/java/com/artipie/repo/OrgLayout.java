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
package com.artipie.repo;

import com.artipie.RepoConfig;
import com.artipie.Settings;
import com.artipie.SliceFromConfig;
import com.artipie.asto.Key;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncSlice;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.SliceSimple;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import org.cactoos.scalar.Unchecked;

/**
 * Hierarchical repositories layout.
 * <p>
 * Artipie installation has multiple namespaces/organizations/users,
 * so first part of the request is a namespace name, second part is a repository name,
 * e.g. URI {@code https://central.artipie.com/public/maven} accesses {@code maven}
 * repository under {@code public} namespace.
 * </p>
 * @see RepoLayout
 * @since 0.4
 * @checkstyle ReturnCountCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class OrgLayout implements RepoLayout {

    /**
     * Repository path prefix.
     */
    private static final Pattern REPO_PREF =
        Pattern.compile("/(?:[^/.]+)/(?:[^/.]+)(/.*)");

    /**
     * Artipie settings.
     */
    private final Settings settings;

    /**
     * Vertx instance.
     */
    private final Vertx vertx;

    /**
     * New org layout.
     * @param settings Artipie settings
     * @param vertx Vertx
     */
    public OrgLayout(final Settings settings, final Vertx vertx) {
        this.settings = settings;
        this.vertx = vertx;
    }

    @Override
    @SuppressWarnings("PMD.OnlyOneReturn")
    public Slice resolve(final String path) {
        final String[] parts = path.replaceAll("^/+", "").split("/");
        if (parts.length < 2) {
            return new SliceSimple(
                new RsWithStatus(
                    new RsWithBody(String.format("Bad request: %s", path), StandardCharsets.UTF_8),
                    RsStatus.BAD_REQUEST
                )
            );
        }
        final String namespace = parts[0];
        final String repo = parts[1];
        Logger.debug(this, "Slice repo=%s", repo);
        final Key.From key = new Key.From(namespace, String.format("%s.yaml", repo));
        return new AsyncSlice(
            CompletableFuture.supplyAsync(
                () -> new Unchecked<>(this.settings::storage).value()
            ).thenCompose(
                storage -> storage.exists(key).thenApply(
                    exist -> {
                        final Slice slice;
                        if (exist) {
                            slice = new AsyncSlice(
                                storage.value(key).thenCombine(
                                    new Unchecked<>(this.settings::auth).value(),
                                    (content, auth) -> new SliceFromConfig(
                                        new RepoConfig(new Key.From(namespace, repo), content),
                                        this.vertx,
                                        auth,
                                        OrgLayout.REPO_PREF
                                    )
                                )
                            );
                        } else {
                            slice = new SliceSimple(
                                new RsWithStatus(
                                    new RsWithBody(
                                        String.format("Repository '%s' was not found", repo),
                                        StandardCharsets.UTF_8
                                    ),
                                    RsStatus.NOT_FOUND
                                )
                            );
                        }
                        return slice;
                    }
                )
            )
        );
    }
}

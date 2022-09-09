/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.misc.UncheckedConsumer;
import com.artipie.settings.StorageAliases;
import com.artipie.settings.cache.StorageConfigCache;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.nio.charset.StandardCharsets;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link StorageAliasesRest} with flat layout.
 * @since 0.27
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
public final class StorageAliasesRestFlatTest extends RestApiServerBase {

    @Test
    void listsCommonAliases(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.save(
            new Key.From(StorageAliases.FILE_NAME),
            this.yamlAliases().getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest("/api/v1/storages"),
            new UncheckedConsumer<>(
                response -> JSONAssert.assertEquals(
                    response.body().toJsonArray().encode(),
                    this.jsonAliases(),
                    true
                )
            )
        );
    }

    @Test
    void listsRepoAliases(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        final String rname = "my-maven";
        this.save(
            new Key.From(rname, StorageAliases.FILE_NAME),
            this.yamlAliases().getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(String.format("/api/v1/repository/%s/storages", rname)),
            new UncheckedConsumer<>(
                response -> JSONAssert.assertEquals(
                    response.body().toJsonArray().encode(),
                    this.jsonAliases(),
                    true
                )
            )
        );
    }

    @Test
    void returnsEmptyArrayIfAliasesDoNotExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest("/api/v1/storages"),
            resp ->
                MatcherAssert.assertThat(
                    resp.body().toJsonArray().isEmpty(), new IsEqual<>(true)
                )
        );
    }

    @Test
    void addsNewCommonAlias(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.requestAndAssert(
            vertx, ctx,
            new TestRequest(
                HttpMethod.PUT, "/api/v1/storages/new-alias",
                new JsonObject().put("type", "file").put("path", "new/alias/path")
            ),
            resp -> {
                MatcherAssert.assertThat(
                    resp.statusCode(), new IsEqual<>(HttpStatus.CREATED_201)
                );
                MatcherAssert.assertThat(
                    new String(
                        this.storage().value(new Key.From(StorageAliases.FILE_NAME)),
                        StandardCharsets.UTF_8
                    ),
                    new IsEqual<>(
                        String.join(
                            System.lineSeparator(),
                            "storages:",
                            "  \"new-alias\":",
                            "    type: file",
                            "    path: new/alias/path"
                        )
                    )
                );
                MatcherAssert.assertThat(
                    "Storages cache was invalidated",
                    ((StorageConfigCache.Fake) this.settingsCaches().storageConfig())
                        .wasInvalidated()
                );
            }
        );
    }

    @Test
    void addsRepoAlias(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        final String rname = "my-pypi";
        this.save(
            new Key.From(rname, StorageAliases.FILE_NAME),
            this.yamlAliases().getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.PUT, String.format("/api/v1/repository/%s/storages/new-alias", rname),
                new JsonObject().put("type", "file").put("path", "new/alias/path")
            ),
            resp -> {
                MatcherAssert.assertThat(
                    resp.statusCode(), new IsEqual<>(HttpStatus.CREATED_201)
                );
                MatcherAssert.assertThat(
                    new String(
                        this.storage().value(new Key.From(rname, StorageAliases.FILE_NAME)),
                        StandardCharsets.UTF_8
                    ),
                    new IsEqual<>(
                        String.join(
                            System.lineSeparator(),
                            "storages:",
                            "  default:",
                            "    type: fs",
                            "    path: /var/artipie/repo/data",
                            "  \"redis-sto\":",
                            "    type: redis",
                            "    config: some",
                            "  \"new-alias\":",
                            "    type: file",
                            "    path: new/alias/path"
                        )
                    )
                );
                MatcherAssert.assertThat(
                    "Storages cache was invalidated",
                    ((StorageConfigCache.Fake) this.settingsCaches().storageConfig())
                        .wasInvalidated()
                );
            }
        );
    }

    @Test
    void returnsNotFoundIfAliasesDoNotExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.DELETE, "/api/v1/storages/any"),
            resp ->
                MatcherAssert.assertThat(resp.statusCode(), new IsEqual<>(HttpStatus.NOT_FOUND_404))
        );
    }

    @Test
    void removesCommonAlias(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.save(
            new Key.From(StorageAliases.FILE_NAME),
            this.yamlAliases().getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.DELETE, "/api/v1/storages/redis-sto"),
            resp -> {
                MatcherAssert.assertThat(resp.statusCode(), new IsEqual<>(HttpStatus.OK_200));
                MatcherAssert.assertThat(
                    new String(
                        this.storage().value(new Key.From(StorageAliases.FILE_NAME)),
                        StandardCharsets.UTF_8
                    ),
                    new IsEqual<>(
                        String.join(
                            System.lineSeparator(),
                            "storages:",
                            "  default:",
                            "    type: fs",
                            "    path: /var/artipie/repo/data"
                        )
                    )
                );
                MatcherAssert.assertThat(
                    "Storages cache was invalidated",
                    ((StorageConfigCache.Fake) this.settingsCaches().storageConfig())
                        .wasInvalidated()
                );
            }
        );
    }

    @Test
    void removesRepoAlias(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        final String rname = "my-rpm";
        this.save(
            new Key.From(rname, StorageAliases.FILE_NAME),
            this.yamlAliases().getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.DELETE, String.format("/api/v1/repository/%s/storages/default", rname)
            ),
            resp -> {
                MatcherAssert.assertThat(resp.statusCode(), new IsEqual<>(HttpStatus.OK_200));
                MatcherAssert.assertThat(
                    new String(
                        this.storage().value(new Key.From(rname, StorageAliases.FILE_NAME)),
                        StandardCharsets.UTF_8
                    ),
                    new IsEqual<>(
                        String.join(
                            System.lineSeparator(),
                            "storages:",
                            "  \"redis-sto\":",
                            "    type: redis",
                            "    config: some"
                        )
                    )
                );
                MatcherAssert.assertThat(
                    "Storages cache was invalidated",
                    ((StorageConfigCache.Fake) this.settingsCaches().storageConfig())
                        .wasInvalidated()
                );
            }
        );
    }

    @Override
    String layout() {
        return "flat";
    }

    private String yamlAliases() {
        return String.join(
            "\n",
            "storages:",
            "  default:",
            "    type: fs",
            "    path: /var/artipie/repo/data",
            "  redis-sto:",
            "    type: redis",
            "    config: some"
        );
    }

    private String jsonAliases() {
        // @checkstyle LineLengthCheck (1 line)
        return "[{\"alias\":\"default\",\"storage\":{\"type\":\"fs\",\"path\":\"/var/artipie/repo/data\"}},{\"alias\":\"redis-sto\",\"storage\":{\"type\":\"redis\",\"config\":\"some\"}}]";
    }
}

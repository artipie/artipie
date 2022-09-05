/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.misc.UncheckedConsumer;
import com.artipie.settings.StorageAliases;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link StorageAliasesRest} with flat layout.
 * @since 0.27
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class StorageAliasesRestFlatTest extends RestApiServerBase {

    @Test
    void listsCommonAliases(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.save(
            new Key.From(StorageAliases.FILE_NAME),
            this.yamlAliases().getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new Request("/api/v1/storages"),
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
            vertx, ctx, new Request(String.format("/api/v1/repository/%s/storages", rname)),
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
            vertx, ctx, new Request("/api/v1/storages"),
            resp ->
                MatcherAssert.assertThat(
                    resp.body().toJsonArray().isEmpty(), new IsEqual<>(true)
                )
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

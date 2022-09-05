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
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link StorageAliasesRest} with org layout.
 * @since 0.27
 */
public final class StorageAliasesRestOrgTest extends RestApiServerBase {

    @Test
    void listsCommonAliases(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
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
    void listsUserAliases(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        final String name = "alice";
        this.save(
            new Key.From(name, StorageAliases.FILE_NAME),
            this.yamlAliases().getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new Request(String.format("/api/v1/storages/%s", name)),
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
        final String name = "alice/docker";
        this.save(
            new Key.From(name, StorageAliases.FILE_NAME),
            this.yamlAliases().getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new Request(String.format("/api/v1/repository/%s/storages", name)),
            new UncheckedConsumer<>(
                response -> JSONAssert.assertEquals(
                    response.body().toJsonArray().encode(),
                    this.jsonAliases(),
                    true
                )
            )
        );
    }

    @Override
    String layout() {
        return "org";
    }

    private String yamlAliases() {
        return String.join(
            "\n",
            "storages:",
            "  local:",
            "    type: fs",
            "    path: /var/artipie/local/data",
            "  s3-sto:",
            "    type: s3",
            "    bucket: any",
            "    region: east",
            "    endpoint: https://minio.selfhosted/s3"
        );
    }

    private String jsonAliases() {
        // @checkstyle LineLengthCheck (1 line)
        return "[{\"alias\":\"local\",\"storage\":{\"type\":\"fs\",\"path\":\"/var/artipie/local/data\"}},{\"alias\":\"s3-sto\",\"storage\":{\"type\":\"s3\",\"bucket\":\"any\",\"region\":\"east\",\"endpoint\":\"https://minio.selfhosted/s3\"}}]";
    }

}

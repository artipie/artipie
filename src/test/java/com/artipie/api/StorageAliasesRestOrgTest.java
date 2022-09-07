/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.misc.UncheckedConsumer;
import com.artipie.settings.StorageAliases;
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
 * Test for {@link StorageAliasesRest} with org layout.
 * @since 0.27
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class StorageAliasesRestOrgTest extends RestApiServerBase {

    @Test
    void listsCommonAliases(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
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
    void listsUserAliases(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        final String name = "alice";
        this.save(
            new Key.From(name, StorageAliases.FILE_NAME),
            this.yamlAliases().getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(String.format("/api/v1/storages/%s", name)),
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
            vertx, ctx, new TestRequest(String.format("/api/v1/repository/%s/storages", name)),
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
    void addsNewUserAlias(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        final String uname = "john";
        this.requestAndAssert(
            vertx, ctx,
            new TestRequest(
                HttpMethod.PUT, String.format("/api/v1/storages/%s/new-alias", uname),
                new JsonObject().put("type", "file").put("path", "john/new/alias/path")
            ),
            resp -> {
                MatcherAssert.assertThat(
                    resp.statusCode(), new IsEqual<>(HttpStatus.CREATED_201)
                );
                MatcherAssert.assertThat(
                    new String(
                        this.storage().value(new Key.From(uname, StorageAliases.FILE_NAME)),
                        StandardCharsets.UTF_8
                    ),
                    new IsEqual<>(
                        String.join(
                            "\n",
                            "storages:",
                            "  \"new-alias\":",
                            "    type: file",
                            "    path: john/new/alias/path"
                        )
                    )
                );
            }
        );
    }

    @Test
    void updatesRepoAlias(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        final String rname = "my-pypi";
        final String uname = "mark";
        final Key.From key = new Key.From(uname, rname, StorageAliases.FILE_NAME);
        this.save(key, this.yamlAliases().getBytes(StandardCharsets.UTF_8));
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.PUT,
                String.format("/api/v1/repository/%s/%s/storages/local", uname, rname),
                new JsonObject().put("type", "file").put("path", "/var/artipie/python/local")
            ),
            resp -> {
                MatcherAssert.assertThat(
                    resp.statusCode(), new IsEqual<>(HttpStatus.CREATED_201)
                );
                MatcherAssert.assertThat(
                    new String(this.storage().value(key), StandardCharsets.UTF_8),
                    new IsEqual<>(
                        String.join(
                            "\n",
                            "storages:",
                            "  local:",
                            "    type: file",
                            "    path: /var/artipie/python/local",
                            "  \"s3-sto\":",
                            "    type: s3",
                            "    bucket: any",
                            "    region: east",
                            "    endpoint: \"https://minio.selfhosted/s3\""
                        )
                    )
                );
            }
        );
    }

    @Test
    void addsNewCommonAlias(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.save(
            new Key.From(StorageAliases.FILE_NAME),
            this.yamlAliases().getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
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
                            "\n",
                            "storages:",
                            "  local:",
                            "    type: fs",
                            "    path: /var/artipie/local/data",
                            "  \"s3-sto\":",
                            "    type: s3",
                            "    bucket: any",
                            "    region: east",
                            "    endpoint: \"https://minio.selfhosted/s3\"",
                            "  \"new-alias\":",
                            "    type: file",
                            "    path: new/alias/path"
                        )
                    )
                );
            }
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

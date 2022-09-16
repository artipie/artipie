/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link RepositoryRest} with `flat` layout.
 * @since 0.26
 * @checkstyle DesignForExtensionCheck (500 lines)
 */
@ExtendWith(VertxExtension.class)
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
public abstract class RepositoryRestBaseTest extends RestApiServerBase {
    /**
     * Temp dir.
     * @checkstyle VisibilityModifierCheck (500 lines)
     */
    @TempDir
    Path temp;

    /**
     * Test data storage.
     */
    private BlockingStorage data;

    /**
     * Getter of test data storage.
     * @return Test data storage
     */
    BlockingStorage getData() {
        return this.data;
    }

    /**
     * Before each method creates test data storage instance.
     */
    @BeforeEach
    void init() {
        this.data = new BlockingStorage(new FileStorage(this.temp));
    }

    /**
     * Provides repository settings for data storage.
     * @return Data storage settings
     */
    String repoSettings() {
        return String.join(
            System.lineSeparator(),
            "repo:",
            "  type: binary",
            "  storage:",
            "    type: fs",
            String.format("    path: %s", this.temp.toString())
        );
    }

    void getRepository(final Vertx vertx, final VertxTestContext ctx,
        final RepositoryName rname)
        throws Exception {
        this.save(new ConfigKeys(rname.toString()).yamlKey(), this.repoSettings().getBytes());
        this.requestAndAssert(
            vertx,
            ctx,
            new TestRequest(
                String.format("/api/v1/repository/%s", rname)
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                )
        );
    }

    void getRepositoryWithDuplicatesSettings(final Vertx vertx, final VertxTestContext ctx,
        final RepositoryName rname)
        throws Exception {
        this.save(new ConfigKeys(rname.toString()).yamlKey(), new byte[0]);
        this.save(new ConfigKeys(rname.toString()).ymlKey(), new byte[0]);
        this.requestAndAssert(
            vertx,
            ctx,
            new TestRequest(
                String.format("/api/v1/repository/%s", rname)
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.CONFLICT_409)
                )
        );
    }

    void getRepositoryNotfound(final Vertx vertx, final VertxTestContext ctx,
        final RepositoryName rname) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(String.format("/api/v1/repository/%s", rname)),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.NOT_FOUND_404)
                )
        );
    }

    void getReservedRepository(final Vertx vertx, final VertxTestContext ctx,
        final RepositoryName rname) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(String.format("/api/v1/repository/%s", rname)),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.BAD_REQUEST_400)
                )
        );
    }

    void createRepository(final Vertx vertx, final VertxTestContext ctx,
        final RepositoryName rname) throws Exception {
        this.requestAndAssert(
            vertx,
            ctx,
            new TestRequest(
                HttpMethod.PUT,
                String.format("/api/v1/repository/%s", rname),
                new JsonObject()
                    .put(
                        "repo", new JsonObject()
                            .put("type", "fs")
                            .put("storage", new JsonObject())
                    )
            ),
            resp -> {
                MatcherAssert.assertThat(
                    resp.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                MatcherAssert.assertThat(
                    this.storage().exists(new ConfigKeys(rname.toString()).yamlKey()),
                    new IsEqual<>(true)
                );
            }
        );
    }

    void createDuplicateRepository(final Vertx vertx, final VertxTestContext ctx,
        final RepositoryName rname) throws Exception {
        this.save(
            new ConfigKeys(rname.toString()).yamlKey(), new byte[0]
        );
        this.requestAndAssert(
            vertx,
            ctx,
            new TestRequest(
                HttpMethod.PUT,
                String.format("/api/v1/repository/%s", rname),
                new JsonObject()
                    .put(
                        "repo", new JsonObject()
                            .put("type", "fs")
                            .put("storage", new JsonObject())
                    )
            ),
            resp ->
                MatcherAssert.assertThat(
                    resp.statusCode(),
                    new IsEqual<>(HttpStatus.CONFLICT_409)
                )
        );
    }

    void createReservedRepository(final Vertx vertx, final VertxTestContext ctx,
        final RepositoryName rname) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.PUT,
                String.format("/api/v1/repository/%s", rname),
                new JsonObject()
                    .put(
                        "repo", new JsonObject()
                            .put("type", "fs")
                            .put("storage", new JsonObject())
                    )
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.BAD_REQUEST_400)
                )
        );
    }

    void deleteRepository(final Vertx vertx, final VertxTestContext ctx, final RepositoryName rname)
        throws Exception {
        this.save(
            new ConfigKeys(rname.toString()).yamlKey(),
            this.repoSettings().getBytes(StandardCharsets.UTF_8)
        );
        final Key.From alpine = new Key.From(
            String.format("%s/alpine.img", rname)
        );
        this.getData().save(alpine, new byte[]{});
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.DELETE,
                String.format("/api/v1/repository/%s", rname)
            ),
            res -> {
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                MatcherAssert.assertThat(
                    waitCondition(
                        () ->
                            !this.storage().exists(
                                new ConfigKeys(rname.toString()).yamlKey()
                            )
                    ),
                    new IsEqual<>(true)
                );
                MatcherAssert.assertThat(
                    waitCondition(() -> !this.getData().exists(alpine)),
                    new IsEqual<>(true)
                );
            }
        );
    }

    void deleteRepositoryNotfound(final Vertx vertx, final VertxTestContext ctx,
        final RepositoryName rname) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.DELETE,
                String.format("/api/v1/repository/%s", rname)
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.NOT_FOUND_404)
                )
        );
    }

    void deleteReservedRepository(final Vertx vertx, final VertxTestContext ctx,
        final RepositoryName rname) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.DELETE, String.format("/api/v1/repository/%s", rname)
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.BAD_REQUEST_400)
                )
        );
    }

    // @checkstyle ParameterNumberCheck (5 lines)
    void moveRepository(final Vertx vertx, final VertxTestContext ctx,
        final RepositoryName rname, final RepositoryName newrname)
        throws Exception {
        this.save(
            new ConfigKeys(rname.toString()).yamlKey(),
            this.repoSettings().getBytes(StandardCharsets.UTF_8)
        );
        final Key.From alpine = new Key.From(
            String.format("%s/alpine.img", rname)
        );
        this.getData().save(alpine, new byte[]{});
        final JsonObject json = new JsonObject()
            .put("name", "docker-repo-new");
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.PUT,
                String.format("/api/v1/repository/%s/move", rname),
                json
            ),
            res -> {
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                MatcherAssert.assertThat(
                    waitCondition(
                        () -> !this.storage().exists(
                            new ConfigKeys(rname.toString()).yamlKey()
                        )
                    ),
                    new IsEqual<>(true)
                );
                MatcherAssert.assertThat(
                    waitCondition(() -> !this.getData().exists(alpine)),
                    new IsEqual<>(true)
                );
                MatcherAssert.assertThat(
                    waitCondition(
                        () ->
                            this.storage().exists(
                                new ConfigKeys(newrname.toString()).yamlKey()
                            )
                    ),
                    new IsEqual<>(true)
                );
                MatcherAssert.assertThat(
                    waitCondition(
                        () ->
                            this.getData().exists(
                                new Key.From(
                                    String.format("%s/alpine.img", newrname)
                                )
                            )
                    ),
                    new IsEqual<>(true)
                );
            }
        );
    }

    void moveRepositoryNotfound(final Vertx vertx, final VertxTestContext ctx,
        final RepositoryName rname)
        throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.PUT,
                String.format("/api/v1/repository/%s/move", rname),
                new JsonObject().put("name", "docker-repo-new")
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.NOT_FOUND_404)
                )
        );
    }

    void moveRepositoryWithDuplicatesSettings(final Vertx vertx, final VertxTestContext ctx,
        final RepositoryName rname)
        throws Exception {
        new ConfigKeys(rname.toString()).keys().forEach(
            key ->
                this.save(
                    key,
                    new byte[0]
                )
        );
        this.requestAndAssert(
            vertx, ctx,
            new TestRequest(
                HttpMethod.PUT,
                String.format("/api/v1/repository/%s/move", rname),
                new JsonObject().put("name", "docker-repo-new")
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.CONFLICT_409)
                )
        );
    }

    void moveRepositoryReservedRepo(final Vertx vertx, final VertxTestContext ctx,
        final RepositoryName rname)
        throws Exception {
        this.requestAndAssert(
            vertx, ctx,
            new TestRequest(
                HttpMethod.PUT,
                String.format("/api/v1/repository/%s/move", rname),
                new JsonObject().put("name", "docker-repo-new")
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.BAD_REQUEST_400)
                )
        );
    }
}

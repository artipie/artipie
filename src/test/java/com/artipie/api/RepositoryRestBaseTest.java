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
 * Test for {@link RepositoryRest}.
 * @since 0.26
 * @checkstyle DesignForExtensionCheck (1000 lines)
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

    void getRepoReturnsOkIfRepositoryExists(final Vertx vertx, final VertxTestContext ctx,
        final RepositoryName rname) throws Exception {
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

    void getRepoReturnsConflictIfRepositoryHasSettingsDuplicates(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname) throws Exception {
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

    void getRepoReturnsNotFoundIfRepositoryDoesNotExist(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(String.format("/api/v1/repository/%s", rname)),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.NOT_FOUND_404)
                )
        );
    }

    void getRepoReturnsBadRequestIfRepositoryHasReservedName(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(String.format("/api/v1/repository/%s", rname)),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.BAD_REQUEST_400)
                )
        );
    }

    void existsRepoReturnsOkIfRepositoryExists(final Vertx vertx, final VertxTestContext ctx,
        final RepositoryName rname) throws Exception {
        this.save(new ConfigKeys(rname.toString()).yamlKey(), this.repoSettings().getBytes());
        this.requestAndAssert(
            vertx,
            ctx,
            new TestRequest(HttpMethod.HEAD, String.format("/api/v1/repository/%s", rname)),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                )
        );
    }

    void existsRepoReturnsConflictIfRepositoryHasSettingsDuplicates(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname) throws Exception {
        this.save(new ConfigKeys(rname.toString()).yamlKey(), new byte[0]);
        this.save(new ConfigKeys(rname.toString()).ymlKey(), new byte[0]);
        this.requestAndAssert(
            vertx,
            ctx,
            new TestRequest(HttpMethod.HEAD, String.format("/api/v1/repository/%s", rname)),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.CONFLICT_409)
                )
        );
    }

    void existsRepoReturnsNotFoundIfRepositoryDoesNotExist(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.HEAD, String.format("/api/v1/repository/%s", rname)
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.NOT_FOUND_404)
                )
        );
    }

    void existsRepoReturnsBadRequestIfRepositoryHasReservedName(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.HEAD, String.format("/api/v1/repository/%s", rname)
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.BAD_REQUEST_400)
                )
        );
    }

    void createRepoReturnsOkIfRepositoryNoExists(final Vertx vertx, final VertxTestContext ctx,
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

    void updateRepoReturnsOkIfRepositoryAlreadyExists(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname) throws Exception {
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
            resp -> {
                MatcherAssert.assertThat(
                    resp.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                MatcherAssert.assertThat(
                    storage().value(new ConfigKeys(rname.toString()).yamlKey()).length > 0,
                    new IsEqual<>(true)
                );
            }
        );
    }

    void createRepoReturnsBadRequestIfRepositoryHasReservedName(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname) throws Exception {
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

    void removeRepoReturnsOkIfRepositoryExists(final Vertx vertx, final VertxTestContext ctx,
        final RepositoryName rname) throws Exception {
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

    void removeRepoReturnsNotFoundIfRepositoryDoesNotExist(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname) throws Exception {
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

    void removeRepoReturnsBadRequestIfRepositoryHasReservedName(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname) throws Exception {
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

    void removeRepoReturnsOkIfRepositoryHasWrongStorageConfiguration(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname) throws Exception {
        final String repoconf = String.join(
            System.lineSeparator(),
            "repo:",
            "  type: binary",
            "  storage: fakeStorage"
        );
        this.removeRepoReturnsOkIfRepositoryHasAnyConfiguration(vertx, ctx, rname, repoconf);
    }

    void removeRepoReturnsOkAndRepoIsRemovedIfRepositoryHasWrongConfiguration(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname) throws Exception {
        final String repoconf = String.join(
            System.lineSeparator(),
            "“When you go after honey with a balloon,",
            " the great thing is to not let the bees know you’re coming.",
            "—Winnie the Pooh"
        );
        this.removeRepoReturnsOkIfRepositoryHasAnyConfiguration(vertx, ctx, rname, repoconf);
    }

    // @checkstyle ParameterNumberCheck (5 lines)
    void removeRepoReturnsOkIfRepositoryHasAnyConfiguration(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname, final String repoconf)
        throws Exception {
        this.save(
            new ConfigKeys(rname.toString()).yamlKey(),
            repoconf.getBytes(StandardCharsets.UTF_8)
        );
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
            }
        );
    }

    // @checkstyle ParameterNumberCheck (5 lines)
    void moveRepoReturnsOkIfRepositoryExists(final Vertx vertx, final VertxTestContext ctx,
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
            .put("new_name", "docker-repo-new");
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

    void moveRepoReturnsNotFoundIfRepositoryDoesNotExist(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.PUT,
                String.format("/api/v1/repository/%s/move", rname),
                new JsonObject().put("new_name", "docker-repo-new")
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.NOT_FOUND_404)
                )
        );
    }

    void moveRepoReturnsConflictIfRepositoryHasSettingsDuplicates(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname) throws Exception {
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
                new JsonObject().put("new_name", "docker-repo-new")
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.CONFLICT_409)
                )
        );
    }

    void moveRepoReturnsBadRequestIfRepositoryHasReservedName(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname) throws Exception {
        this.requestAndAssert(
            vertx, ctx,
            new TestRequest(
                HttpMethod.PUT,
                String.format("/api/v1/repository/%s/move", rname),
                new JsonObject().put("new_name", "docker-repo-new")
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.BAD_REQUEST_400)
                )
        );
    }

    // @checkstyle ParameterNumberCheck (3 lines)
    void moveRepoReturnsBadRequestIfNewRepositoryHasReservedName(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname, final RepositoryName newrname)
        throws Exception {
        this.save(
            new ConfigKeys(rname.toString()).yamlKey(),
            this.repoSettings().getBytes(StandardCharsets.UTF_8)
        );
        this.requestAndAssert(
            vertx, ctx,
            new TestRequest(
                HttpMethod.PUT,
                String.format("/api/v1/repository/%s/move", rname),
                new JsonObject()
                    .put("new_name", newrname.toString())
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.BAD_REQUEST_400)
                )
        );
    }

    // @checkstyle ParameterNumberCheck (3 lines)
    void moveRepoReturnsBadRequestIfNewRepositoryHasSettingsDuplicates(final Vertx vertx,
        final VertxTestContext ctx, final RepositoryName rname, final String newrname)
        throws Exception {
        this.save(
            new ConfigKeys(rname.toString()).yamlKey(),
            this.repoSettings().getBytes(StandardCharsets.UTF_8)
        );
        final String newrnamepath;
        if ("flat".equals(layout())) {
            newrnamepath = newrname;
        } else {
            newrnamepath = String.format("Alice/%s", newrname);
        }
        new ConfigKeys(newrnamepath).keys().forEach(
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
                new JsonObject().put("new_name", newrname)
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.CONFLICT_409)
                )
        );
    }
}

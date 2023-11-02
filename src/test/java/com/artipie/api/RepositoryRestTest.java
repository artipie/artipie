/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.test.TestFiltersCache;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.awaitility.Awaitility;
import org.eclipse.jetty.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link RepositoryRest}.
 * @since 0.26
 * @checkstyle DesignForExtensionCheck (1000 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (1000 lines)
 */
@ExtendWith(VertxExtension.class)
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
public final class RepositoryRestTest extends RestApiServerBase {
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
            String.format("    path: %s", this.temp.toString()),
            "  filters:",
            "    include:",
            "      glob:",
            "        - filter: '**/*'",
            "    exclude:"
        );
    }

    @Test
    void listsRepos(final Vertx vertx, final VertxTestContext ctx) throws Exception {
        this.save(new Key.From("docker-repo.yaml"), new byte[0]);
        this.save(new Key.From("rpm-local.yml"), new byte[0]);
        this.save(new Key.From("conda-remote.yml"), new byte[0]);
        this.requestAndAssert(
            vertx, ctx, new TestRequest("/api/v1/repository/list"),
            resp -> MatcherAssert.assertThat(
                resp.body().toJsonArray().stream().collect(Collectors.toList()),
                Matchers.containsInAnyOrder(
                    "rpm-local", "docker-repo", "conda-remote"
                )
            )
        );
    }

    @Test
    void getRepoReturnsOkIfRepoExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final RepositoryName rname = new RepositoryName.Simple("docker-repo");
        this.save(new ConfigKeys(rname.toString()).yamlKey(), this.repoSettings().getBytes());
        this.requestAndAssert(
            vertx, ctx, new TestRequest(String.format("/api/v1/repository/%s", rname)),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                )
        );
    }

    @Test
    void getRepoReturnsConflictIfRepoHasSettingsDuplicates(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        final RepositoryName rname = new RepositoryName.Simple("docker-repo");
        this.save(new ConfigKeys(rname.toString()).yamlKey(), new byte[0]);
        this.save(new ConfigKeys(rname.toString()).ymlKey(), new byte[0]);
        this.requestAndAssert(
            vertx, ctx, new TestRequest(String.format("/api/v1/repository/%s", rname)),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.CONFLICT_409)
                )
        );
    }

    @Test
    void getRepoReturnsNotFoundIfRepoDoesNotExist(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest("/api/v1/repository/docker-repo"),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.NOT_FOUND_404)
                )
        );
    }

    @Test
    void getRepoReturnsBadRequestIfRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest("/api/v1/repository/_storages"),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.BAD_REQUEST_400)
                )
        );
    }

    @Test
    void existsRepoReturnsOkIfRepoExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final RepositoryName rname = new RepositoryName.Simple("docker-repo");
        this.save(new ConfigKeys(rname.toString()).yamlKey(), this.repoSettings().getBytes());
        this.requestAndAssert(
            vertx, ctx,
            new TestRequest(HttpMethod.HEAD, String.format("/api/v1/repository/%s", rname)),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                )
        );
    }

    @Test
    void existsRepoReturnsConflictIfRepoHasSettingsDuplicates(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        final RepositoryName rname = new RepositoryName.Simple("docker-repo");
        this.save(new ConfigKeys(rname.toString()).yamlKey(), new byte[0]);
        this.save(new ConfigKeys(rname.toString()).ymlKey(), new byte[0]);
        this.requestAndAssert(
            vertx, ctx,
            new TestRequest(HttpMethod.HEAD, String.format("/api/v1/repository/%s", rname)),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.CONFLICT_409)
                )
        );
    }

    @Test
    void existsRepoReturnsNotFoundIfRepoDoesNotExist(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.HEAD, "/api/v1/repository/docker-repo"),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.NOT_FOUND_404)
                )
        );
    }

    @Test
    void existsRepoReturnsBadRequestIfRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.HEAD, "/api/v1/repository/_storages"),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.BAD_REQUEST_400)
                )
        );
    }

    @Test
    void createRepoReturnsOkIfRepoNoExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final RepositoryName rname = new RepositoryName.Simple("docker-repo");
        this.requestAndAssert(
            vertx, ctx,
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
                MatcherAssert.assertThat(
                    "Filters cache should be invalidated",
                    ((TestFiltersCache) this.settingsCaches().filtersCache()).wasInvalidated()
                );
            }
        );
    }

    @Test
    void updateRepoReturnsOkIfRepoAlreadyExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final RepositoryName rname = new RepositoryName.Simple("docker-repo");
        this.save(new ConfigKeys(rname.toString()).yamlKey(), new byte[0]);
        this.requestAndAssert(
            vertx, ctx,
            new TestRequest(
                HttpMethod.PUT,
                String.format("/api/v1/repository/%s", rname),
                new JsonObject().put(
                    "repo", new JsonObject().put("type", "fs").put("storage", new JsonObject())
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
                MatcherAssert.assertThat(
                    "Filters cache should be invalidated",
                    ((TestFiltersCache) this.settingsCaches().filtersCache()).wasInvalidated()
                );
            }
        );
    }

    @Test
    void createRepoReturnsBadRequestIfRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.PUT, "/api/v1/repository/_storages",
                new JsonObject().put(
                    "repo", new JsonObject().put("type", "fs").put("storage", new JsonObject())
                )
            ),
            res -> MatcherAssert.assertThat(
                res.statusCode(),
                new IsEqual<>(HttpStatus.BAD_REQUEST_400)
            )
        );
    }

    @Test
    void removeRepoReturnsOkIfRepoExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final RepositoryName rname = new RepositoryName.Simple("docker-repo");
        this.save(
            new ConfigKeys(rname.toString()).yamlKey(),
            this.repoSettings().getBytes(StandardCharsets.UTF_8)
        );
        final Key.From alpine = new Key.From(String.format("%s/alpine.img", rname));
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
                Awaitility.waitAtMost(MAX_WAIT_TIME, TimeUnit.SECONDS).until(
                    () -> !this.storage().exists(new ConfigKeys(rname.toString()).yamlKey())
                );
                Awaitility.waitAtMost(MAX_WAIT_TIME, TimeUnit.SECONDS).until(
                    () -> !this.getData().exists(alpine)
                );
                MatcherAssert.assertThat(
                    "Filters cache should be invalidated",
                    ((TestFiltersCache) this.settingsCaches().filtersCache()).wasInvalidated()
                );
            }
        );
    }

    @Test
    void removeRepoReturnsNotFoundIfRepoDoesNotExist(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.DELETE,
                String.format("/api/v1/repository/%s", new RepositoryName.Simple("docker-repo"))
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.NOT_FOUND_404)
                )
        );
    }

    @Test
    void removeRepoReturnsBadRequestIfRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(HttpMethod.DELETE, "/api/v1/repository/_storages"),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.BAD_REQUEST_400)
                )
        );
    }

    @Test
    void removeRepoReturnsOkIfRepoHasWrongStorageConfiguration(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        final String repoconf = String.join(
            System.lineSeparator(),
            "repo:",
            "  type: binary",
            "  storage: fakeStorage"
        );
        final RepositoryName rname = new RepositoryName.Simple("docker");
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
                Awaitility.waitAtMost(MAX_WAIT_TIME, TimeUnit.SECONDS).until(
                    () -> !this.storage().exists(new ConfigKeys(rname.toString()).yamlKey())
                );
            }
        );
    }

    @Test
    void removeRepoReturnsOkAndRepoIsRemovedIfRepoHasWrongConfiguration(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        final String repoconf = String.join(
            System.lineSeparator(),
            "“When you go after honey with a balloon,",
            " the great thing is to not let the bees know you’re coming.",
            "—Winnie the Pooh"
        );
        final RepositoryName rname = new RepositoryName.Simple("docker");
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
                Awaitility.waitAtMost(MAX_WAIT_TIME, TimeUnit.SECONDS).until(
                    () -> !this.storage().exists(new ConfigKeys(rname.toString()).yamlKey())
                );
            }
        );
    }

    @Test
    void moveRepoReturnsOkIfRepoExists(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        final RepositoryName rname = new RepositoryName.Simple("docker-repo");
        final RepositoryName newrname = new RepositoryName.Simple("docker-repo-new");
        this.save(
            new ConfigKeys(rname.toString()).yamlKey(),
            this.repoSettings().getBytes(StandardCharsets.UTF_8)
        );
        final Key.From alpine = new Key.From(String.format("%s/alpine.img", rname));
        this.getData().save(alpine, new byte[]{});
        final JsonObject json = new JsonObject().put("new_name", "docker-repo-new");
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.PUT, String.format("/api/v1/repository/%s/move", rname), json
            ),
            res -> {
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.OK_200)
                );
                Awaitility.waitAtMost(MAX_WAIT_TIME, TimeUnit.SECONDS).until(
                    () -> !this.storage().exists(new ConfigKeys(rname.toString()).yamlKey())
                );
                Awaitility.waitAtMost(MAX_WAIT_TIME, TimeUnit.SECONDS).until(
                    () -> !this.getData().exists(alpine)
                );
                Awaitility.waitAtMost(MAX_WAIT_TIME, TimeUnit.SECONDS).until(
                    () -> this.storage().exists(new ConfigKeys(newrname.toString()).yamlKey())
                );
                Awaitility.waitAtMost(MAX_WAIT_TIME, TimeUnit.SECONDS).until(
                    () -> this.getData().exists(
                        new Key.From(String.format("%s/alpine.img", newrname))
                    )
                );
                MatcherAssert.assertThat(
                    "Filters cache should be invalidated",
                    ((TestFiltersCache) this.settingsCaches().filtersCache()).wasInvalidated()
                );
            }
        );
    }

    @Test
    void moveRepoReturnsNotFoundIfRepoDoesNotExist(final Vertx vertx, final VertxTestContext ctx)
        throws Exception {
        this.requestAndAssert(
            vertx, ctx, new TestRequest(
                HttpMethod.PUT,
                String.format(
                    "/api/v1/repository/%s/move", new RepositoryName.Simple("docker-repo")
                ),
                new JsonObject().put("new_name", "docker-repo-new")
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.NOT_FOUND_404)
                )
        );
    }

    @Test
    void moveRepoReturnsConflictIfRepoHasSettingsDuplicates(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        final RepositoryName rname = new RepositoryName.Simple("docker-repo");
        new ConfigKeys(rname.toString()).keys().forEach(key -> this.save(key, new byte[0]));
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

    @Test
    void moveRepoReturnsBadRequestIfRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        this.requestAndAssert(
            vertx, ctx,
            new TestRequest(
                HttpMethod.PUT,
                "/api/v1/repository/_storages/move",
                new JsonObject().put("new_name", "docker-repo-new")
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.BAD_REQUEST_400)
                )
        );
    }

    @Test
    void moveRepoReturnsBadRequestIfNewRepoHasReservedName(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        final RepositoryName rname = new RepositoryName.Simple("doker-repo");
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
                    .put("new_name", "_storages")
            ),
            res ->
                MatcherAssert.assertThat(
                    res.statusCode(),
                    new IsEqual<>(HttpStatus.BAD_REQUEST_400)
                )
        );
    }

    @Test
    void moveRepoReturnsBadRequestIfNewRepoHasSettingsDuplicates(final Vertx vertx,
        final VertxTestContext ctx) throws Exception {
        final RepositoryName rname = new RepositoryName.Simple("doker-repo");
        final String newrname = "docker-repo-new";
        this.save(
            new ConfigKeys(rname.toString()).yamlKey(),
            this.repoSettings().getBytes(StandardCharsets.UTF_8)
        );
        new ConfigKeys(newrname).keys().forEach(key -> this.save(key, new byte[0]));
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

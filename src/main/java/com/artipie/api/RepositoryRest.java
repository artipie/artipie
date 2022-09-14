/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.settings.RepoData;
import com.artipie.settings.repo.CrudRepoSettings;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import org.eclipse.jetty.http.HttpStatus;

/**
 * Rest-api operations for repositories settings CRUD
 * (create/read/update/delete) operations.
 * @since 0.26
 */
@SuppressWarnings("PMD.OnlyOneReturn")
public final class RepositoryRest extends BaseRest {
    /**
     * Repository settings create/read/update/delete.
     */
    private final CrudRepoSettings crs;

    /**
     * Repository data management.
     */
    private final RepoData data;

    /**
     * Artipie layout.
     */
    private final String layout;

    /**
     * Ctor.
     * @param crs Repository settings create/read/update/delete
     * @param data Repository data management
     * @param layout Artipie layout
     */
    public RepositoryRest(final CrudRepoSettings crs, final RepoData data, final String layout) {
        this.crs = crs;
        this.data = data;
        this.layout = layout;
    }

    @Override
    public void init(final RouterBuilder rbr) {
        rbr.operation("listAll")
            .handler(this::listAll)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        if ("flat".equals(this.layout)) {
            rbr.operation("getRepo")
                .handler(this::getRepo)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rbr.operation("createRepo")
                .handler(this::createRepo)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rbr.operation("removeRepo")
                .handler(this::removeRepo)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        } else {
            rbr.operation("list")
                .handler(this::listUserRepos)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rbr.operation("getUserRepo")
                .handler(this::getRepo)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rbr.operation("createUserRepo")
                .handler(this::createRepo)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rbr.operation("removeUserRepo")
                .handler(this::removeRepo)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        }
    }

    /**
     * Get a repository settings json.
     * @param context Routing context
     * @checkstyle ReturnCountCheck (20 lines)
     */
    private void getRepo(final RoutingContext context) {
        final RepositoryName rname = new RepositoryName.FromRequest(context, this.layout);
        final RepositoryNameCondition rnvalidator = new RepositoryNameCondition(rname);
        final Validator validator = validator(
            validator(
                rnvalidator,
                rnvalidator,
                HttpStatus.BAD_REQUEST_400
            ),
            validator(
                () -> this.crs.exists(rname),
                () -> String.format("Repository %s does not exist.", rname),
                HttpStatus.NOT_FOUND_404
            ),
            validator(
                () -> !this.crs.hasSettingsDuplicates(rname),
                () -> String.format(
                    new StringBuilder()
                        .append("Repository %s has settings duplicates. ")
                        .append("Please remove repository and create it again.")
                        .toString(),
                    rname
                ),
                HttpStatus.CONFLICT_409
            )
        );
        if (validator.validate(context)) {
            context.response()
                .setStatusCode(HttpStatus.OK_200)
                .end(this.crs.value(rname).toString());
        }
    }

    /**
     * List all existing repositories.
     * @param context Routing context
     */
    private void listAll(final RoutingContext context) {
        context.response().setStatusCode(HttpStatus.OK_200).end(
            JsonArray.of(this.crs.listAll().toArray()).encode()
        );
    }

    /**
     * List user's repositories.
     * @param context Routing context
     */
    private void listUserRepos(final RoutingContext context) {
        context.response().setStatusCode(HttpStatus.OK_200).end(
            JsonArray.of(this.crs.list(context.pathParam(RepositoryName.UNAME)).toArray()).encode()
        );
    }

    /**
     * Create a repository.
     * @param context Routing context
     */
    private void createRepo(final RoutingContext context) {
        final RepositoryName rname = new RepositoryName.FromRequest(context, this.layout);
        final RepositoryNameCondition rnvalidator = new RepositoryNameCondition(rname);
        final Validator validator = validator(
            validator(
                rnvalidator,
                rnvalidator,
                HttpStatus.BAD_REQUEST_400
            ),
            validator(
                () -> !this.crs.exists(rname),
                () -> String.format("Repository %s already exists", rname),
                HttpStatus.CONFLICT_409
            )
        );
        if (validator.validate(context)) {
            final JsonObject json = (JsonObject) (Json.createReader(
                new StringReader(context.body().asString())
            ).read());
            final String repomsg = "Section `repo` is required";
            final Validator jsvalidator = validator(
                validator(
                    () -> json != null,
                    () -> "JSON body is expected",
                    HttpStatus.BAD_REQUEST_400
                ),
                validator(
                    () -> json.containsKey(RepositoryRest.REPO),
                    () -> repomsg,
                    HttpStatus.BAD_REQUEST_400
                ),
                validator(
                    () -> json.getJsonObject(RepositoryRest.REPO) != null,
                    () -> repomsg,
                    HttpStatus.BAD_REQUEST_400
                ),
                validator(
                    () -> json.getJsonObject(RepositoryRest.REPO).containsKey("type"),
                    () -> "Repository type is required",
                    HttpStatus.BAD_REQUEST_400
                ),
                validator(
                    () -> json.getJsonObject(RepositoryRest.REPO).containsKey("storage"),
                    () -> "Repository storage is required",
                    HttpStatus.BAD_REQUEST_400
                )
            );
            if (jsvalidator.validate(context)) {
                this.crs.save(rname, json);
                context.response()
                    .setStatusCode(HttpStatus.OK_200)
                    .end();
            }
        }
    }

    /**
     * Remove a repository settings json and repository data.
     * @param context Routing context
     */
    private void removeRepo(final RoutingContext context) {
        final RepositoryName rname = new RepositoryName.FromRequest(context, this.layout);
        final RepositoryNameCondition rnvalidator = new RepositoryNameCondition(rname);
        final Validator validator = validator(
            validator(
                rnvalidator,
                rnvalidator,
                HttpStatus.BAD_REQUEST_400
            ),
            validator(
                () -> this.crs.exists(rname),
                () -> String.format("Repository %s does not exist. ", rname),
                HttpStatus.NOT_FOUND_404
            )
        );
        if (validator.validate(context)) {
            this.data.remove(rname).thenRun(() -> this.crs.delete(rname));
            context.response()
                .setStatusCode(HttpStatus.OK_200)
                .end();
        }
    }
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.api.verifier.ExistenceVerifier;
import com.artipie.api.verifier.ReservedNamesVerifier;
import com.artipie.api.verifier.SettingsDuplicatesVerifier;
import com.artipie.settings.Layout;
import com.artipie.settings.RepoData;
import com.artipie.settings.repo.CrudRepoSettings;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import javax.json.JsonObject;
import org.eclipse.jetty.http.HttpStatus;

/**
 * Rest-api operations for repositories settings CRUD
 * (create/read/update/delete) operations.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
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
            rbr.operation("moveRepo")
                .handler(this::moveRepo)
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
            rbr.operation("moveUserRepo")
                .handler(this::moveRepo)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        }
    }

    /**
     * Get a repository settings json.
     * @param context Routing context
     */
    private void getRepo(final RoutingContext context) {
        final RepositoryName rname = new RepositoryName.FromRequest(context, this.layout);
        final ReservedNamesVerifier reserved = new ReservedNamesVerifier(rname);
        final SettingsDuplicatesVerifier duplicates =
            new SettingsDuplicatesVerifier(rname, this.crs);
        final ExistenceVerifier existence = new ExistenceVerifier(rname, this.crs);
        final Validator validator = new Validator.All(
            Validator.validator(reserved::valid, reserved::message, HttpStatus.BAD_REQUEST_400),
            Validator.validator(existence::valid, existence::message, HttpStatus.NOT_FOUND_404),
            Validator.validator(duplicates::valid, duplicates::message, HttpStatus.CONFLICT_409)
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
        final ReservedNamesVerifier reserved = new ReservedNamesVerifier(rname);
        final Validator validator = new Validator.All(
            Validator.validator(reserved::valid, reserved::message, HttpStatus.BAD_REQUEST_400),
            Validator.validator(
                () -> !this.crs.exists(rname),
                () -> String.format("Repository %s already exists", rname),
                HttpStatus.CONFLICT_409
            )
        );
        if (validator.validate(context)) {
            final JsonObject json = BaseRest.readJsonObject(context);
            final String repomsg = "Section `repo` is required";
            final Validator jsvalidator = new Validator.All(
                Validator.validator(
                    () -> json != null,
                    "JSON body is expected",
                    HttpStatus.BAD_REQUEST_400
                ),
                Validator.validator(
                    () -> json.containsKey(RepositoryRest.REPO),
                    repomsg,
                    HttpStatus.BAD_REQUEST_400
                ),
                Validator.validator(
                    () -> json.getJsonObject(RepositoryRest.REPO) != null,
                    repomsg,
                    HttpStatus.BAD_REQUEST_400
                ),
                Validator.validator(
                    () -> json.getJsonObject(RepositoryRest.REPO).containsKey("type"),
                    "Repository type is required",
                    HttpStatus.BAD_REQUEST_400
                ),
                Validator.validator(
                    () -> json.getJsonObject(RepositoryRest.REPO).containsKey("storage"),
                    "Repository storage is required",
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
        final ReservedNamesVerifier reserved = new ReservedNamesVerifier(rname);
        final Validator validator = new Validator.All(
            Validator.validator(reserved::valid, reserved::message, HttpStatus.BAD_REQUEST_400),
            Validator.validator(
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

    /**
     * Move a repository settings.
     * @param context Routing context
     */
    private void moveRepo(final RoutingContext context) {
        final RepositoryName rname = new RepositoryName.FromRequest(context, this.layout);
        ReservedNamesVerifier reserved = new ReservedNamesVerifier(rname);
        SettingsDuplicatesVerifier duplicates = new SettingsDuplicatesVerifier(rname, this.crs);
        final ExistenceVerifier existence = new ExistenceVerifier(rname, this.crs);
        Validator validator = new Validator.All(
            Validator.validator(reserved::valid, reserved::message, HttpStatus.BAD_REQUEST_400),
            Validator.validator(existence::valid, existence::message, HttpStatus.NOT_FOUND_404),
            Validator.validator(duplicates::valid, duplicates::message, HttpStatus.CONFLICT_409)
        );
        if (validator.validate(context)) {
            final String newname = BaseRest.readJsonObject(context).getString("new_name");
            final RepositoryName newrname;
            if (new Layout.Flat().toString().equals(this.layout)) {
                newrname = new RepositoryName.Flat(newname);
            } else {
                newrname = new RepositoryName.Org(newname, context.pathParam(RepositoryName.UNAME));
            }
            reserved = new ReservedNamesVerifier(newrname);
            duplicates = new SettingsDuplicatesVerifier(newrname, this.crs);
            validator = new Validator.All(
                Validator.validator(reserved::valid, reserved::message, HttpStatus.BAD_REQUEST_400),
                Validator.validator(duplicates::valid, duplicates::message, HttpStatus.CONFLICT_409)
            );
            if (validator.validate(context)) {
                this.data.move(rname, newrname).thenRun(() -> this.crs.move(rname, newrname));
                context.response()
                    .setStatusCode(HttpStatus.OK_200)
                    .end();
            }
        }
    }
}

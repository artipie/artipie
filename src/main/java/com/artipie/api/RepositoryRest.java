///*
// * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
// * https://github.com/artipie/artipie/LICENSE.txt
// */

package com.artipie.api;

import com.artipie.api.verifier.ExistenceVerifier;
import com.artipie.api.verifier.ReservedNamesVerifier;
import com.artipie.api.verifier.SettingsDuplicatesVerifier;
import com.artipie.settings.Layout;
import com.artipie.settings.RepoData;
import com.artipie.settings.repo.CrudRepoSettings;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.json.JsonArray;


import javax.json.JsonObject;

import org.eclipse.jetty.http.HttpStatus;

public final class RepositoryRest extends BaseRest {
    private final CrudRepoSettings crs;
    private final RepoData data;
    private final String layout;

    public RepositoryRest(final CrudRepoSettings crs, final RepoData data, final String layout) {
        this.crs = crs;
        this.data = data;
        this.layout = layout;
    }

    @Override
    protected void initRoutes(final RouterBuilder rbr) {
        rbr.operation("listAll")
                .handler(this::listAll)
                .failureHandler(new ErrorHandler(500));

        if ("flat".equals(this.layout)) {
            rbr.operation("getRepo")
                    .handler(this::getRepo)
                    .failureHandler(new ErrorHandler(500));
            rbr.operation("existRepo")
                    .handler(this::existRepo)
                    .failureHandler(new ErrorHandler(500));
            rbr.operation("createOrUpdateRepo")
                    .handler(this::createOrUpdateRepo)
                    .failureHandler(new ErrorHandler(500));
            rbr.operation("removeRepo")
                    .handler(this::removeRepo)
                    .failureHandler(new ErrorHandler(500));
            rbr.operation("moveRepo")
                    .handler(this::moveRepo)
                    .failureHandler(new ErrorHandler(500));
        } else {
            rbr.operation("list")
                    .handler(this::listUserRepos)
                    .failureHandler(new ErrorHandler(500));
            rbr.operation("getUserRepo")
                    .handler(this::getRepo)
                    .failureHandler(new ErrorHandler(500));
            rbr.operation("existUserRepo")
                    .handler(this::existRepo)
                    .failureHandler(new ErrorHandler(500));
            rbr.operation("createOrUpdateUserRepo")
                    .handler(this::createOrUpdateRepo)
                    .failureHandler(new ErrorHandler(500));
            rbr.operation("removeUserRepo")
                    .handler(this::removeRepo)
                    .failureHandler(new ErrorHandler(500));
            rbr.operation("moveUserRepo")
                    .handler(this::moveRepo)
                    .failureHandler(new ErrorHandler(500));
        }
    }

    private void getRepo(final RoutingContext context) {
        final RepositoryName rname = new RepositoryName.FromRequest(context, this.layout);
        final Validator validator = new Validator.All(
                Validator.validator(new ReservedNamesVerifier(rname), HttpStatus.BAD_REQUEST_400),
                Validator.validator(new ExistenceVerifier(rname, this.crs), HttpStatus.NOT_FOUND_404),
                Validator.validator(
                        new SettingsDuplicatesVerifier(rname, this.crs),
                        HttpStatus.CONFLICT_409
                )
        );
        if (validator.validate(context)) {
            context.response()
                    .setStatusCode(HttpStatus.OK_200)
                    .end(this.crs.value(rname).toString());
        }
    }

    private void existRepo(final RoutingContext context) {
        final RepositoryName rname = new RepositoryName.FromRequest(context, this.layout);
        final Validator validator = new Validator.All(
                Validator.validator(new ReservedNamesVerifier(rname), HttpStatus.BAD_REQUEST_400),
                Validator.validator(new ExistenceVerifier(rname, this.crs), HttpStatus.NOT_FOUND_404),
                Validator.validator(
                        new SettingsDuplicatesVerifier(rname, this.crs),
                        HttpStatus.CONFLICT_409
                )
        );
        if (validator.validate(context)) {
            context.response()
                    .setStatusCode(HttpStatus.OK_200)
                    .end();
        }
    }

    private void listAll(final RoutingContext context) {
        context.response().setStatusCode(HttpStatus.OK_200).end(
                JsonArray.of(this.crs.listAll().toArray()).encode()
        );
    }

    private void listUserRepos(final RoutingContext context) {
        context.response().setStatusCode(HttpStatus.OK_200).end(
                JsonArray.of(this.crs.list(context.pathParam(RepositoryName.USER_NAME)).toArray())
                        .encode()
        );
    }

    private void createOrUpdateRepo(final RoutingContext context) {
        final RepositoryName rname = new RepositoryName.FromRequest(context, this.layout);
        final Validator validator = new Validator.All(
                Validator.validator(new ReservedNamesVerifier(rname), HttpStatus.BAD_REQUEST_400)
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

    private void removeRepo(final RoutingContext context) {
        final RepositoryName rname = new RepositoryName.FromRequest(context, this.layout);
        final Validator validator = new Validator.All(
                Validator.validator(new ReservedNamesVerifier(rname), HttpStatus.BAD_REQUEST_400),
                Validator.validator(
                        () -> this.crs.exists(rname),
                        () -> String.format("Repository %s does not exist. ", rname),
                        HttpStatus.NOT_FOUND_404
                )
        );
        if (validator.validate(context)) {
            this.data.remove(rname)
                    .thenRun(() -> this.crs.delete(rname))
                    .exceptionally(
                            exc -> {
                                this.crs.delete(rname);
                                return null;
                            }
                    );
            context.response()
                    .setStatusCode(HttpStatus.OK_200)
                    .end();
        }
    }

    private void moveRepo(final RoutingContext context) {
        final RepositoryName rname = new RepositoryName.FromRequest(context, this.layout);
        Validator validator = new Validator.All(
                Validator.validator(new ReservedNamesVerifier(rname), HttpStatus.BAD_REQUEST_400),
                Validator.validator(new ExistenceVerifier(rname, this.crs), HttpStatus.NOT_FOUND_404),
                Validator.validator(
                        new SettingsDuplicatesVerifier(rname, this.crs),
                        HttpStatus.CONFLICT_409
                )
        );
        if (validator.validate(context)) {
            final String newname = BaseRest.readJsonObject(context).getString("new_name");
            final RepositoryName newrname;
            if (new Layout.Flat().toString().equals(this.layout)) {
                newrname = new RepositoryName.Flat(newname);
            } else {
                newrname = new RepositoryName.Org(
                        newname, context.pathParam(RepositoryName.USER_NAME)
                );
            }
            validator = new Validator.All(
                    Validator.validator(
                            new ReservedNamesVerifier(newrname),
                            HttpStatus.BAD_REQUEST_400
                    ),
                    Validator.validator(
                            new SettingsDuplicatesVerifier(newrname, this.crs),
                            HttpStatus.CONFLICT_409
                    )
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



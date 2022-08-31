/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.misc.JavaResource;
import com.artipie.settings.repo.CrudRepoSettings;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import org.eclipse.jetty.http.HttpStatus;

/**
 * Rest-api operations for repositories settings CRUD
 * (create/read/update/delete) operations.
 * @since 0.26
 */
@SuppressWarnings({"PMD.OnlyOneReturn", "PMD.AvoidDuplicateLiterals"})
public final class RepositoryRest extends BaseRest {

    /**
     * Username path parameter name.
     */
    private static final String UNAME = "uname";

    /**
     * Repository path parameter name.
     */
    private static final String RNAME = "rname";

    /**
     * Repository settings create/read/update/delete.
     */
    private final CrudRepoSettings crs;

    /**
     * Artipie layout.
     */
    private final String layout;

    /**
     * Ctor.
     * @param crs Repository settings create/read/update/delete
     * @param layout Artipie layout
     */
    public RepositoryRest(final CrudRepoSettings crs, final String layout) {
        this.crs = crs;
        this.layout = layout;
    }

    @Override
    public void init(final RouterBuilder rbr) {
        rbr.operation("listAll")
            .handler(this::listAll)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("list")
            .handler(this::listUserRepos)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("getRepo")
            .handler(this::getRepo)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("getUserRepo")
            .handler(this::getRepo)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("createRepo")
            .handler(this::createRepo)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        rbr.operation("createUserRepo")
            .handler(this::createRepo)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        final Router router = rbr.createRouter();
        router.route("/api/*").handler(
            StaticHandler.create(
                FileSystemAccess.ROOT,
                new JavaResource("swagger-ui").uri().getPath()
            )
        );
    }

    /**
     * Get a repository settings json.
     * @param context Routing context
     */
    private void getRepo(final RoutingContext context) {
        final RepositoryName rname;
        if ("flat".equals(this.layout)) {
            rname = new RepositoryName.FlatRepositoryName(
                context.pathParam(RepositoryRest.RNAME)
            );
        } else {
            rname = new RepositoryName.OrgRepositoryName(
                context.pathParam(RepositoryRest.UNAME),
                context.pathParam(RepositoryRest.RNAME)
            );
        }
        if (!this.crs.exists(rname)) {
            context.response()
                .setStatusCode(HttpStatus.NOT_FOUND_404)
                .end(String.format("Repository %s does not exist", rname));
            return;
        }
        context.response().setStatusCode(HttpStatus.OK_200).end(
            this.crs.value(rname).toBuffer()
        );
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
        if ("flat".equals(this.layout)) {
            context.response()
                .setStatusCode(HttpStatus.CONFLICT_409)
                .end("List user repositories is not allowed for 'flat' layout");
            return;
        }
        final String uname = context.pathParam(RepositoryRest.UNAME);
        context.response().setStatusCode(HttpStatus.OK_200).end(
            JsonArray.of(this.crs.list(uname).toArray()).encode()
        );
    }

    /**
     * Create a repository.
     * @param context Routing context
     */
    private void createRepo(final RoutingContext context) {
        final RepositoryName rname;
        if ("flat".equals(this.layout)) {
            rname = new RepositoryName.FlatRepositoryName(
                context.pathParam(RepositoryRest.RNAME)
            );
        } else {
            rname = new RepositoryName.OrgRepositoryName(
                context.pathParam(RepositoryRest.UNAME),
                context.pathParam(RepositoryRest.RNAME)
            );
        }
        if (this.crs.exists(rname)) {
            context.response()
                .setStatusCode(HttpStatus.CONFLICT_409)
                .end(String.format("Repository %s already exists", rname.string()));
            return;
        }
        final JsonObject json = context.body().asJsonObject();
        if (RepositoryRest.validateRepo(context, json)) {
            this.crs.save(rname, json);
            context.response()
                .setStatusCode(HttpStatus.OK_200)
                .end();
        }
    }

    /**
     * Validate new repository json.
     * @param context Routing context
     * @param json New repository json
     * @return True is json correct
     * @checkstyle ReturnCountCheck (20 lines)
     */
    private static boolean validateRepo(final RoutingContext context, final JsonObject json) {
        if (json == null) {
            context.response()
                .setStatusCode(HttpStatus.BAD_REQUEST_400)
                .end("JSON body is expected");
            return false;
        }
        if (!json.containsKey("repo")) {
            context.response()
                .setStatusCode(HttpStatus.BAD_REQUEST_400)
                .end("Section `repo` is required");
            return false;
        }
        final Object obj = json.getValue("repo");
        if (!(obj instanceof JsonObject)) {
            context.response()
                .setStatusCode(HttpStatus.BAD_REQUEST_400)
                .end("Section `repo` should be in json format");
            return false;
        }
        final JsonObject repo = (JsonObject) obj;
        if (repo.getString("type") == null) {
            context.response()
                .setStatusCode(HttpStatus.BAD_REQUEST_400)
                .end("Section `type` is required");
            return false;
        }
        if (!repo.containsKey("storage")) {
            context.response()
                .setStatusCode(HttpStatus.BAD_REQUEST_400)
                .end("Section `storage` is required");
            return false;
        }
        if (!(repo.getValue("storage") instanceof JsonObject)) {
            context.response()
                .setStatusCode(HttpStatus.BAD_REQUEST_400)
                .end("Section `storage` should be in json format");
            return false;
        }
        return true;
    }
}

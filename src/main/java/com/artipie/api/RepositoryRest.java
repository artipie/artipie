/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.misc.JavaResource;
import com.artipie.settings.repo.CrudRepoSettings;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.io.StringReader;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.spi.JsonProvider;
import org.eclipse.jetty.http.HttpStatus;

/**
 * Rest-api operations for repositories settings CRUD
 * (create/read/update/delete) operations.
 * @since 0.26
 */
@SuppressWarnings("PMD.OnlyOneReturn")
public final class RepositoryRest extends BaseRest {
    /**
     * Key 'repo' inside json-object.
     */
    private static final String REPO = "repo";

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
        if ("flat".equals(this.layout)) {
            rbr.operation("getRepo")
                .handler(this::getRepo)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rbr.operation("createRepo")
                .handler(this::createRepo)
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
        }
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
     * @checkstyle ReturnCountCheck (20 lines)
     */
    private void getRepo(final RoutingContext context) {
        final RepositoryName rname = new RepositoryName(context, this.layout);
        if (!RepositoryName.allowedReponame(rname)) {
            context.response()
                .setStatusCode(HttpStatus.BAD_REQUEST_400)
                .end(wrongReponame(rname));
            return;
        }
        if (!this.crs.exists(rname)) {
            context.response()
                .setStatusCode(HttpStatus.NOT_FOUND_404)
                .end(String.format("Repository %s does not exist. ", rname));
            return;
        }
        context.response()
            .setStatusCode(HttpStatus.OK_200)
            .end(this.crs.value(rname).toString());
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
        final String uname = context.pathParam(RepositoryName.UNAME);
        context.response().setStatusCode(HttpStatus.OK_200).end(
            JsonArray.of(this.crs.list(uname).toArray()).encode()
        );
    }

    /**
     * Create a repository.
     * @param context Routing context
     * @checkstyle ReturnCountCheck (20 lines)
     */
    private void createRepo(final RoutingContext context) {
        final RepositoryName rname = new RepositoryName(context, this.layout);
        if (!RepositoryName.allowedReponame(rname)) {
            context.response()
                .setStatusCode(HttpStatus.BAD_REQUEST_400)
                .end(wrongReponame(rname));
            return;
        }
        if (this.crs.exists(rname)) {
            context.response()
                .setStatusCode(HttpStatus.CONFLICT_409)
                .end(String.format("Repository %s already exists", rname));
            return;
        }
        final JsonStructure json = JsonProvider.provider().createReader(
            new StringReader(context.body().asString())
        ).read();
        if (RepositoryRest.validateRepo(context, (JsonObject) json)) {
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
        final String repomsg = "Section `repo` is required";
        if (!json.containsKey(RepositoryRest.REPO)) {
            context.response()
                .setStatusCode(HttpStatus.BAD_REQUEST_400)
                .end(repomsg);
            return false;
        }
        final JsonObject repo = json.getJsonObject(RepositoryRest.REPO);
        if (repo == null) {
            context.response()
                .setStatusCode(HttpStatus.BAD_REQUEST_400)
                .end(repomsg);
            return false;
        }
        if (!repo.containsKey("type")) {
            context.response()
                .setStatusCode(HttpStatus.BAD_REQUEST_400)
                .end("Repository type is required");
            return false;
        }
        if (!repo.containsKey("storage")) {
            context.response()
                .setStatusCode(HttpStatus.BAD_REQUEST_400)
                .end("Repository storage is required");
            return false;
        }
        return true;
    }

    /**
     * Message for 'wrong repository name'.
     * @param rname Repository name
     * @return Message description
     */
    private static String wrongReponame(final RepositoryName rname) {
        return
            new StringBuilder()
                .append(String.format("Wrong repository name '%s'. ", rname))
                .append("Repository name should not include following words: ")
                .append(RepositoryName.RESTRICTED_REPOS)
                .toString();
    }
}

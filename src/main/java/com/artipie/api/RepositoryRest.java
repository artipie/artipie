/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.api.perms.ApiRepositoryPermission;
import com.artipie.api.verifier.ExistenceVerifier;
import com.artipie.api.verifier.ReservedNamesVerifier;
import com.artipie.api.verifier.SettingsDuplicatesVerifier;
import com.artipie.http.auth.AuthUser;
import com.artipie.security.policy.Policy;
import com.artipie.settings.Layout;
import com.artipie.settings.RepoData;
import com.artipie.settings.cache.FiltersCache;
import com.artipie.settings.repo.CrudRepoSettings;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.openapi.RouterBuilder;
import java.security.PermissionCollection;
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
     * Update repo permission.
     */
    private static final ApiRepositoryPermission UPDATE =
        new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.UPDATE);

    /**
     * Create repo permission.
     */
    private static final ApiRepositoryPermission CREATE =
        new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.CREATE);

    /**
     * Artipie filters cache.
     */
    private final FiltersCache cache;

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
     * Artipie policy.
     */
    private final Policy<?> policy;

    /**
     * Ctor.
     * @param cache Artipie filters cache
     * @param crs Repository settings create/read/update/delete
     * @param data Repository data management
     * @param layout Artipie layout
     * @param policy Artipie policy
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public RepositoryRest(final FiltersCache cache, final CrudRepoSettings crs, final RepoData data,
        final String layout, final Policy<?> policy) {
        this.cache = cache;
        this.crs = crs;
        this.data = data;
        this.layout = layout;
        this.policy = policy;
    }

    @Override
    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public void init(final RouterBuilder rbr) {
        rbr.operation("listAll")
            .handler(
                new AuthzHandler(
                    this.policy,
                    new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.READ)
                )
            )
            .handler(this::listAll)
            .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        if ("flat".equals(this.layout)) {
            rbr.operation("getRepo")
                .handler(
                    new AuthzHandler(
                        this.policy,
                        new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.READ)
                    )
                )
                .handler(this::getRepo)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rbr.operation("existRepo")
                .handler(
                    new AuthzHandler(
                        this.policy,
                        new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.READ)
                    )
                )
                .handler(this::existRepo)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rbr.operation("createOrUpdateRepo")
                .handler(this::createOrUpdateRepo)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rbr.operation("removeRepo")
                .handler(
                    new AuthzHandler(
                        this.policy,
                        new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.DELETE)
                    )
                )
                .handler(this::removeRepo)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rbr.operation("moveRepo")
                .handler(
                    new AuthzHandler(
                        this.policy,
                        new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.MOVE)
                    )
                )
                .handler(this::moveRepo)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
        } else {
            rbr.operation("list")
                .handler(
                    new AuthzHandler(
                        this.policy,
                        new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.READ)
                    )
                )
                .handler(this::listUserRepos)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rbr.operation("getUserRepo")
                .handler(
                    new AuthzHandler(
                        this.policy,
                        new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.READ)
                    )
                )
                .handler(this::getRepo)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rbr.operation("existUserRepo")
                .handler(
                    new AuthzHandler(
                        this.policy,
                        new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.READ)
                    )
                )
                .handler(this::existRepo)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rbr.operation("createOrUpdateUserRepo")
                .handler(this::createOrUpdateRepo)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rbr.operation("removeUserRepo")
                .handler(
                    new AuthzHandler(
                        this.policy,
                        new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.DELETE)
                    )
                )
                .handler(this::removeRepo)
                .failureHandler(this.errorHandler(HttpStatus.INTERNAL_SERVER_ERROR_500));
            rbr.operation("moveUserRepo")
                .handler(
                    new AuthzHandler(
                        this.policy,
                        new ApiRepositoryPermission(ApiRepositoryPermission.RepositoryAction.MOVE)
                    )
                )
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

    /**
     * Checks if repository settings exist.
     * @param context Routing context
     */
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
            JsonArray.of(this.crs.list(context.pathParam(RepositoryName.USER_NAME)).toArray())
                .encode()
        );
    }

    /**
     * Create a repository.
     * @param context Routing context
     */
    private void createOrUpdateRepo(final RoutingContext context) {
        final RepositoryName rname = new RepositoryName.FromRequest(context, this.layout);
        final Validator validator = new Validator.All(
            Validator.validator(new ReservedNamesVerifier(rname), HttpStatus.BAD_REQUEST_400)
        );
        final boolean exists = this.crs.exists(rname);
        final PermissionCollection perms = this.policy.getPermissions(
            new AuthUser(
                context.user().principal().getString(AuthTokenRest.SUB),
                context.user().principal().getString(AuthTokenRest.CONTEXT)
            )
        );
        // @checkstyle BooleanExpressionComplexityCheck (5 lines)
        if ((exists && perms.implies(RepositoryRest.UPDATE)
            || !exists && perms.implies(RepositoryRest.CREATE)) && validator.validate(context)) {
            final JsonObject json = BaseRest.readJsonObject(context);
            final String repomsg = "Section `repo` is required";
            final Validator jsvalidator = new Validator.All(
                Validator.validator(
                    () -> json != null, "JSON body is expected",
                    HttpStatus.BAD_REQUEST_400
                ),
                Validator.validator(
                    () -> json.containsKey(RepositoryRest.REPO), repomsg,
                    HttpStatus.BAD_REQUEST_400
                ),
                Validator.validator(
                    () -> json.getJsonObject(RepositoryRest.REPO) != null, repomsg,
                    HttpStatus.BAD_REQUEST_400
                ),
                Validator.validator(
                    () -> json.getJsonObject(RepositoryRest.REPO).containsKey("type"),
                    "Repository type is required", HttpStatus.BAD_REQUEST_400
                ),
                Validator.validator(
                    () -> json.getJsonObject(RepositoryRest.REPO).containsKey("storage"),
                    "Repository storage is required", HttpStatus.BAD_REQUEST_400
                )
            );
            if (jsvalidator.validate(context)) {
                this.crs.save(rname, json);
                this.cache.invalidate(rname.toString());
                context.response().setStatusCode(HttpStatus.OK_200).end();
            }
        } else {
            context.response().setStatusCode(HttpStatus.FORBIDDEN_403).end();
        }
    }

    /**
     * Remove a repository settings json and repository data.
     * @param context Routing context
     */
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
            this.cache.invalidate(rname.toString());
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
                this.cache.invalidate(rname.toString());
                context.response()
                    .setStatusCode(HttpStatus.OK_200)
                    .end();
            }
        }
    }
}

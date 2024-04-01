/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.BearerAuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.auth.TokenAuthentication;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceDownload;
import com.artipie.http.slice.SliceSimple;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.policy.Policy;

import java.net.URL;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

/**
 * NpmSlice is a http layer in npm adapter.
 *
 * @todo #340:30min Implement `/npm` endpoint properly: for now `/npm` simply returns 200 OK
 *  status without any body. We need to figure out what information can (or should) be returned
 *  by registry on this request and add it. Here are several links that might be useful
 *  https://github.com/npm/cli
 *  https://github.com/npm/registry
 *  https://docs.npmjs.com/cli/v8
 */
@SuppressWarnings("PMD.ExcessiveMethodLength")
public final class NpmSlice implements Slice {

    /**
     * Anonymous token auth for test purposes.
     */
    static final TokenAuthentication ANONYMOUS = tkn
        -> CompletableFuture.completedFuture(Optional.of(new AuthUser("anonymous", "anonymity")));

    /**
     * Header name `npm-command`.
     */
    private static final String NPM_COMMAND = "npm-command";

    /**
     * Header name `referer`.
     */
    private static final String REFERER = "referer";

    /**
     * Route.
     */
    private final SliceRoute route;

    /**
     * Ctor with existing front and default parameters for free access.
     * @param base Base URL.
     * @param storage Storage for package
     */
    public NpmSlice(final URL base, final Storage storage) {
        this(base, storage, Policy.FREE, NpmSlice.ANONYMOUS, "*", Optional.empty());
    }

    /**
     * Ctor with existing front and default parameters for free access.
     * @param base Base URL.
     * @param storage Storage for package
     * @param events Events queue
     */
    public NpmSlice(final URL base, final Storage storage, final Queue<ArtifactEvent> events) {
        this(base, storage, Policy.FREE, NpmSlice.ANONYMOUS, "*", Optional.of(events));
    }

    /**
     * Ctor.
     *
     * @param base Base URL.
     * @param storage Storage for package.
     * @param policy Access permissions.
     * @param auth Authentication.
     * @param name Repository name
     * @param events Events queue
     */
    public NpmSlice(
        final URL base,
        final Storage storage,
        final Policy<?> policy,
        final TokenAuthentication auth,
        final String name,
        final Optional<Queue<ArtifactEvent>> events
    ) {
        this.route = new SliceRoute(
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.GET),
                    new RtRule.ByPath("/npm")
                ),
                new BearerAuthzSlice(
                    new SliceSimple(ResponseBuilder.ok().build()),
                    auth,
                    new OperationControl(
                        policy, new AdapterBasicPermission(name, Action.Standard.READ)
                    )
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.PUT),
                    new RtRule.ByPath(AddDistTagsSlice.PTRN)
                ),
                new BearerAuthzSlice(
                    new AddDistTagsSlice(storage),
                    auth,
                    new OperationControl(
                        policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
                    )
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.DELETE),
                    new RtRule.ByPath(AddDistTagsSlice.PTRN)
                ),
                new BearerAuthzSlice(
                    new DeleteDistTagsSlice(storage),
                    auth,
                    new OperationControl(
                        policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
                    )
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.PUT),
                    new RtRule.Any(
                        new RtRule.ByHeader(NpmSlice.NPM_COMMAND, CliPublish.HEADER),
                        new RtRule.ByHeader(NpmSlice.REFERER, CliPublish.HEADER)
                    )
                ),
                new BearerAuthzSlice(
                    new UploadSlice(new CliPublish(storage), storage, events, name),
                    auth,
                    new OperationControl(
                        policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
                    )
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.PUT),
                    new RtRule.Any(
                        new RtRule.ByHeader(NpmSlice.NPM_COMMAND, DeprecateSlice.HEADER),
                        new RtRule.ByHeader(NpmSlice.REFERER, DeprecateSlice.HEADER)
                    )
                ),
                new BearerAuthzSlice(
                    new DeprecateSlice(storage),
                    auth,
                    new OperationControl(
                        policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
                    )
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.PUT),
                    new RtRule.Any(
                        new RtRule.ByHeader(NpmSlice.NPM_COMMAND, UnpublishPutSlice.HEADER),
                        new RtRule.ByHeader(NpmSlice.REFERER, UnpublishPutSlice.HEADER)
                    )
                ),
                new BearerAuthzSlice(
                    new UnpublishPutSlice(storage, events, name),
                    auth,
                    new OperationControl(
                        policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
                    )
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.PUT),
                    new RtRule.ByPath(CurlPublish.PTRN)
                ),
                new BearerAuthzSlice(
                    new UploadSlice(new CurlPublish(storage), storage, events, name),
                    auth,
                    new OperationControl(
                        policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
                    )
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.GET),
                    new RtRule.ByPath(".*/dist-tags$")
                ),
                new BearerAuthzSlice(
                    new GetDistTagsSlice(storage),
                    auth,
                    new OperationControl(
                        policy, new AdapterBasicPermission(name, Action.Standard.READ)
                    )
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.GET),
                    new RtRule.ByPath(".*(?<!\\.tgz)$")
                ),
                new BearerAuthzSlice(
                    new DownloadPackageSlice(base, storage),
                    auth,
                    new OperationControl(
                        policy, new AdapterBasicPermission(name, Action.Standard.READ)
                    )
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.GET),
                    new RtRule.ByPath(".*\\.tgz$")
                ),
                new BearerAuthzSlice(
                    new SliceDownload(storage),
                    auth,
                    new OperationControl(
                        policy, new AdapterBasicPermission(name, Action.Standard.READ)
                    )
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.DELETE),
                    new RtRule.ByPath(UnpublishForceSlice.PTRN)
                ),
                new BearerAuthzSlice(
                    new UnpublishForceSlice(storage, events, name),
                    auth,
                    new OperationControl(
                        policy, new AdapterBasicPermission(name, Action.Standard.DELETE)
                    )
                )
            )
        );
    }

    @Override
    public CompletableFuture<Response> response(
        final RequestLine line,
        final Headers headers,
        final Content body) {
        return this.route.response(line, headers, body);
    }
}

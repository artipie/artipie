/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.docker.perms.DockerActions;
import com.artipie.docker.perms.DockerRepositoryPermission;
import com.artipie.http.auth.BasicAuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.rq.RequestLine;
import com.artipie.security.perms.EmptyPermissions;
import com.artipie.security.perms.FreePermissions;
import com.artipie.settings.Settings;
import org.apache.http.client.utils.URIBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Slice decorator which redirects all Docker V2 API requests to Artipie format paths.
 */
public final class DockerRoutingSlice implements Slice {

    /**
     * Real path header name.
     */
    private static final String HDR_REAL_PATH = "X-RealPath";

    /**
     * Docker V2 API path pattern.
     */
    private static final Pattern PTN_PATH = Pattern.compile("/v2((/.*)?)");

    /**
     * Origin slice.
     */
    private final Slice origin;

    /**
     * Settings.
     */
    private final Settings settings;

    /**
     * Decorates slice with Docker V2 API routing.
     * @param settings Settings.
     * @param origin Origin slice
     */
    DockerRoutingSlice(final Settings settings, final Slice origin) {
        this.settings = settings;
        this.origin = origin;
    }

    @Override
    @SuppressWarnings("PMD.NestedIfDepthCheck")
    public CompletableFuture<Response> response(
        RequestLine line, Headers headers, Content body
    ) {
        final String path = line.uri().getPath();
        final Matcher matcher = PTN_PATH.matcher(path);
        if (matcher.matches()) {
            final String group = matcher.group(1);
            if (group.isEmpty() || group.equals("/")) {
                return new BasicAuthzSlice(
                    (l, h, b) -> ResponseBuilder.ok()
                        .header("Docker-Distribution-API-Version", "registry/2.0")
                        .completedFuture(),
                    this.settings.authz().authentication(),
                    new OperationControl(
                        user -> user.isAnonymous() ? EmptyPermissions.INSTANCE
                            : FreePermissions.INSTANCE,
                        new DockerRepositoryPermission("*", "*", DockerActions.PULL.mask())
                    )
                ).response(line, headers, body);
            } else {
                return this.origin.response(
                    new RequestLine(
                        line.method().toString(),
                        new URIBuilder(line.uri()).setPath(group).toString(),
                        line.version()
                    ),
                    headers.copy().add(DockerRoutingSlice.HDR_REAL_PATH, path),
                    body
                );
            }
        }
        return this.origin.response(line, headers, body);
    }

    /**
     * Slice which reverts real path from headers if exists.
     * @since 0.9
     */
    public static final class Reverted implements Slice {

        /**
         * Origin slice.
         */
        private final Slice origin;

        /**
         * New {@link Slice} decorator to revert real path.
         * @param origin Origin slice
         */
        public Reverted(final Slice origin) {
            this.origin = origin;
        }

        @Override
        public CompletableFuture<Response> response(final RequestLine line,
                                                    final Headers headers,
                                                    final Content body) {
            return this.origin.response(
                new RequestLine(
                    line.method().toString(),
                    new URIBuilder(line.uri())
                        .setPath(String.format("/v2%s", line.uri().getPath()))
                        .toString(),
                    line.version()
                ),
                headers,
                body
            );
        }
    }
}

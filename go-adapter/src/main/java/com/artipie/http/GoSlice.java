/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.headers.ContentType;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.http.slice.SliceDownload;
import com.artipie.http.slice.SliceSimple;
import com.artipie.http.slice.SliceWithHeaders;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.policy.Policy;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Slice implementation that provides HTTP API (Go module proxy protocol) for Golang repository.
 */
public final class GoSlice implements Slice {

    private final Slice origin;

    /**
     * @param storage Storage
     * @param policy Security policy
     * @param users Users
     * @param name Repository name
     */
    public GoSlice(final Storage storage, final Policy<?> policy, final Authentication users,
        final String name) {
        this.origin = new SliceRoute(
            GoSlice.pathGet(
                ".+/@v/v.*\\.info",
                GoSlice.createSlice(storage, ContentType.json(), policy, users, name)
            ),
            GoSlice.pathGet(
                ".+/@v/v.*\\.mod",
                GoSlice.createSlice(storage, ContentType.text(), policy, users, name)
            ),
            GoSlice.pathGet(
                ".+/@v/v.*\\.zip",
                GoSlice.createSlice(storage, ContentType.mime("application/zip"), policy, users, name)
            ),
            GoSlice.pathGet(
                ".+/@v/list", GoSlice.createSlice(storage, ContentType.text(), policy, users, name)
            ),
            GoSlice.pathGet(
                ".+/@latest",
                new BasicAuthzSlice(
                    new LatestSlice(storage),
                    users,
                    new OperationControl(
                        policy, new AdapterBasicPermission(name, Action.Standard.READ)
                    )
                )
            ),
            new RtRulePath(
                RtRule.FALLBACK,
                new SliceSimple(ResponseBuilder.notFound().build())
            )
        );
    }

    @Override
    public CompletableFuture<ResponseImpl> response(
        final RequestLine line, final Headers headers,
        final Content body) {
        return this.origin.response(line, headers, body);
    }

    /**
     * Creates slice instance.
     * @param storage Storage
     * @param contentType Content-type
     * @param policy Security policy
     * @param users Users
     * @param name Repository name
     * @return Slice
     */
    private static Slice createSlice(
        Storage storage,
        Header contentType,
        Policy<?> policy,
        Authentication users,
        String name
    ) {
        return new BasicAuthzSlice(
            new SliceWithHeaders(new SliceDownload(storage), Headers.from(contentType)),
            users,
            new OperationControl(policy, new AdapterBasicPermission(name, Action.Standard.READ))
        );
    }

    /**
     * This method simply encapsulates all the RtRule instantiations.
     * @param pattern Route pattern
     * @param slice Slice implementation
     * @return Path route slice
     */
    private static RtRulePath pathGet(final String pattern, final Slice slice) {
        return new RtRulePath(
            new RtRule.All(
                new RtRule.ByPath(Pattern.compile(pattern)),
                new ByMethodsRule(RqMethod.GET)
            ),
            new LoggingSlice(slice)
        );
    }
}

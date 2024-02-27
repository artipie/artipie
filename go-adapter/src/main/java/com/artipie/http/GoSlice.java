/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Storage;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
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
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Slice implementation that provides HTTP API (Go module proxy protocol) for Golang repository.
 */
public final class GoSlice implements Slice {

    /**
     * Text header.
     */
    private static final String TEXT_PLAIN = "text/plain";

    /**
     * Origin.
     */
    private final Slice origin;

    /**
     * Ctor.
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
                GoSlice.createSlice(storage, "application/json", policy, users, name)
            ),
            GoSlice.pathGet(
                ".+/@v/v.*\\.mod",
                GoSlice.createSlice(storage, GoSlice.TEXT_PLAIN, policy, users, name)
            ),
            GoSlice.pathGet(
                ".+/@v/v.*\\.zip",
                GoSlice.createSlice(storage, "application/zip", policy, users, name)
            ),
            GoSlice.pathGet(
                ".+/@v/list", GoSlice.createSlice(storage, GoSlice.TEXT_PLAIN, policy, users, name)
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
                new SliceSimple(
                    new RsWithStatus(RsStatus.NOT_FOUND)
                )
            )
        );
    }

    @Override
    public Response response(
        final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return this.origin.response(line, headers, body);
    }

    /**
     * Creates slice instance.
     * @param storage Storage
     * @param type Content-type
     * @param policy Security policy
     * @param users Users
     * @param name Repository name
     * @return Slice
     */
    private static Slice createSlice(final Storage storage, final String type,
        final Policy<?> policy, final Authentication users, final String name) {
        return new BasicAuthzSlice(
            new SliceWithHeaders(
                new SliceDownload(storage),
                new Headers.From("content-type", type)
            ),
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

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.hex.http;

import com.artipie.asto.Storage;
import com.artipie.http.Slice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceSimple;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.policy.Policy;
import java.util.Optional;
import java.util.Queue;

/**
 * Artipie {@link Slice} for HexPm repository HTTP API.
 *
 * @since 0.1
 */
public final class HexSlice extends Slice.Wrap {

    /**
     * Ctor with default parameters for free access.
     *
     * @param storage The storage for package.
     */
    public HexSlice(final Storage storage) {
        this(
            storage,
            Policy.FREE,
            Authentication.ANONYMOUS,
            Optional.empty(),
            "*"
        );
    }

    /**
     * Ctor.
     *
     * @param storage The storage for package.
     * @param policy Access policy.
     * @param users Concrete identities.
     * @param events Artifact events queue
     * @param name Repository name
     */
    public HexSlice(final Storage storage, final Policy<?> policy, final Authentication users,
        final Optional<Queue<ArtifactEvent>> events, final String name) {
        super(new SliceRoute(
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.GET),
                    new RtRule.Any(
                        new RtRule.ByPath(DownloadSlice.PACKAGES_PTRN),
                        new RtRule.ByPath(DownloadSlice.TARBALLS_PTRN)
                    )
                ),
                new BasicAuthzSlice(
                    new DownloadSlice(storage),
                    users,
                    new OperationControl(
                        policy, new AdapterBasicPermission(name, Action.Standard.READ)
                    )
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.GET),
                    new RtRule.ByPath(UserSlice.USERS)
                ),
                new BasicAuthzSlice(
                    new UserSlice(),
                    users,
                    new OperationControl(
                        policy, new AdapterBasicPermission(name, Action.Standard.READ)
                    )
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.POST),
                    new RtRule.ByPath(UploadSlice.PUBLISH)
                ),
                new BasicAuthzSlice(
                    new UploadSlice(storage, events, name),
                    users,
                    new OperationControl(
                        policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
                    )
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.POST),
                    new RtRule.ByPath(DocsSlice.DOCS_PTRN)
                ),
                new BasicAuthzSlice(
                    new DocsSlice(),
                    users,
                    new OperationControl(
                        policy, new AdapterBasicPermission(name, Action.Standard.READ)
                    )
                )
            ),
            new RtRulePath(
                RtRule.FALLBACK, new SliceSimple(StandardRs.NOT_FOUND)
            )
        )
        );
    }
}

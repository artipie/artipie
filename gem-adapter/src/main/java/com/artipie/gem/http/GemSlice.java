/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem.http;

import com.artipie.asto.Storage;
import com.artipie.gem.GemApiKeyAuth;
import com.artipie.http.Slice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.AuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
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
import java.util.Optional;
import java.util.Queue;

/**
 * A slice, which servers gem packages.
 *
 *  Ruby HTTP layer.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 * @since 0.1
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
public final class GemSlice extends Slice.Wrap {

    /**
     * Ctor.
     *
     * @param storage The storage.
     */
    public GemSlice(final Storage storage) {
        this(
            storage,
            Policy.FREE,
            (usr, pwd) -> Optional.of(Authentication.ANONYMOUS),
            "",
            Optional.empty()
        );
    }

    /**
     * Ctor.
     *
     * @param storage The storage.
     * @param events Artifact events queue
     */
    public GemSlice(final Storage storage, final Optional<Queue<ArtifactEvent>> events) {
        this(
            storage,
            Policy.FREE,
            (usr, pwd) -> Optional.of(Authentication.ANONYMOUS),
            "",
            events
        );
    }

    /**
     * Ctor.
     *
     * @param storage The storage.
     * @param policy The policy.
     * @param auth The auth.
     * @param name Repository name
     */
    public GemSlice(
        final Storage storage,
        final Policy<?> policy,
        final Authentication auth,
        final String name
    ) {
        this(storage, policy, auth, name, Optional.empty());
    }

    /**
     * Ctor.
     *
     * @param storage The storage.
     * @param policy The policy.
     * @param auth The auth.
     * @param name Repository name
     * @param events Artifact events queue
     */
    public GemSlice(
        final Storage storage,
        final Policy<?> policy,
        final Authentication auth,
        final String name,
        final Optional<Queue<ArtifactEvent>> events
    ) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new RtRule.All(
                        ByMethodsRule.Standard.POST,
                        new RtRule.ByPath("/api/v1/gems")
                    ),
                    new AuthzSlice(
                        new SubmitGemSlice(storage, events, name),
                        new GemApiKeyAuth(auth),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        ByMethodsRule.Standard.GET,
                        new RtRule.ByPath("/api/v1/dependencies")
                    ),
                    new DepsGemSlice(storage)
                ),
                new RtRulePath(
                    new RtRule.All(
                        ByMethodsRule.Standard.GET,
                        new RtRule.ByPath("/api/v1/api_key")
                    ),
                    new ApiKeySlice(auth)
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.GET),
                        new RtRule.ByPath(ApiGetSlice.PATH_PATTERN)
                    ),
                    new ApiGetSlice(storage)
                ),
                new RtRulePath(
                    new ByMethodsRule(RqMethod.GET),
                    new AuthzSlice(
                        new SliceDownload(storage),
                        new GemApiKeyAuth(auth),
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    RtRule.FALLBACK,
                    new SliceSimple(new RsWithStatus(RsStatus.NOT_FOUND))
                )
            )
        );
    }
}

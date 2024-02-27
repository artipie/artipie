/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.http;

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
import com.artipie.http.slice.SliceDownload;
import com.artipie.http.slice.SliceSimple;
import com.artipie.rpm.RepoConfig;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.policy.Policy;
import java.util.Optional;
import java.util.Queue;

/**
 * Artipie {@link Slice} for RPM repository HTTP API.
 * @since 0.7
 */
public final class RpmSlice extends Slice.Wrap {

    /**
     * Ctor.
     * @param storage Storage
     * @param policy Access policy.
     * @param auth Auth details.
     * @param config Repository configuration.
     */
    public RpmSlice(
        final Storage storage,
        final Policy<?> policy,
        final Authentication auth,
        final RepoConfig config
    ) {
        this(storage, policy, auth, config, Optional.empty());
    }

    /**
     * Ctor.
     * @param storage Storage
     * @param policy Access policy.
     * @param auth Auth details.
     * @param config Repository configuration.
     * @param events Artifact events queue
     */
    public RpmSlice(
        final Storage storage,
        final Policy<?> policy,
        final Authentication auth,
        final RepoConfig config,
        final Optional<Queue<ArtifactEvent>> events
    ) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new ByMethodsRule(RqMethod.GET),
                    new BasicAuthzSlice(
                        new SliceDownload(storage),
                        auth,
                        new OperationControl(
                            policy, new AdapterBasicPermission(config.name(), Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new ByMethodsRule(RqMethod.PUT),
                    new BasicAuthzSlice(
                        new RpmUpload(storage, config, events),
                        auth,
                        new OperationControl(
                            policy, new AdapterBasicPermission(config.name(), Action.Standard.WRITE)
                        )
                    )
                ),
                new RtRulePath(
                    new ByMethodsRule(RqMethod.DELETE),
                    new BasicAuthzSlice(
                        new RpmRemove(storage, config, events),
                        auth,
                        new OperationControl(
                            policy, new AdapterBasicPermission(config.name(), Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(RtRule.FALLBACK, new SliceSimple(StandardRs.NOT_FOUND))
            )
        );
    }
}

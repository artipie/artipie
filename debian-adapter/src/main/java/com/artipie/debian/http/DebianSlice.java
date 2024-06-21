/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian.http;

import com.artipie.asto.Storage;
import com.artipie.debian.Config;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Slice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.rt.MethodRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.*;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.policy.Policy;

import java.util.Optional;
import java.util.Queue;
import java.util.regex.Pattern;

/**
 * Debian slice.
 */
public final class DebianSlice extends Slice.Wrap {

    /**
     * Ctor.
     * @param storage Storage
     * @param policy Policy
     * @param users Users
     * @param config Repository configuration
     * @param events Artifact events queue
     */
    public DebianSlice(
            final Storage storage,
            final Policy<?> policy,
            final Authentication users,
            final Config config,
            final Optional<Queue<ArtifactEvent>> events
    ) {
        super(
            new SliceRoute(
                new RtRulePath(
                    MethodRule.GET,
                    new BasicAuthzSlice(
                        new ReleaseSlice(new SliceDownload(storage), storage, config),
                        users,
                        new OperationControl(
                            policy,
                            new AdapterBasicPermission(config.codename(), Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.Any(
                        MethodRule.PUT, MethodRule.POST
                    ),
                    new BasicAuthzSlice(
                        new ReleaseSlice(new UpdateSlice(storage, config, events), storage, config),
                        users,
                        new OperationControl(
                            policy,
                            new AdapterBasicPermission(config.codename(), Action.Standard.WRITE)
                        )
                    )
                ),
                new RtRulePath(
                    MethodRule.DELETE,
                    new BasicAuthzSlice(
                        new DeleteSlice(storage, config),
                        users,
                        new OperationControl(
                            policy,
                            new AdapterBasicPermission(config.codename(), Action.Standard.WRITE)
                        )
                    )
                ),
                new RtRulePath(
                    RtRule.FALLBACK, new SliceSimple(ResponseBuilder.notFound().build())
                )
            )
        );
    }
}

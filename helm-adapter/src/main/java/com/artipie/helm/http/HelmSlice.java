/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm.http;

import com.artipie.asto.Storage;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Slice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.rt.MethodRule;
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
 * HelmSlice.
 * @since 0.1
 */
public final class HelmSlice extends Slice.Wrap {

    /**
     * Ctor.
     *
     * @param storage The storage.
     * @param base The base path the slice is expected to be accessed from. Example: https://central.artipie.com/helm
     * @param policy Access policy.
     * @param auth Authentication.
     * @param name Repository name
     * @param events Events queue
     */
    public HelmSlice(
        final Storage storage,
        final String base,
        final Policy<?> policy,
        final Authentication auth,
        final String name,
        final Optional<Queue<ArtifactEvent>> events
    ) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new RtRule.Any(
                        MethodRule.PUT, MethodRule.POST
                    ),
                    new BasicAuthzSlice(
                        new PushChartSlice(storage, events, name),
                        auth,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        MethodRule.GET,
                        new RtRule.ByPath(DownloadIndexSlice.PTRN)
                    ),
                    new BasicAuthzSlice(
                        new DownloadIndexSlice(base, storage),
                        auth,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    MethodRule.GET,
                    new BasicAuthzSlice(
                        new SliceDownload(storage),
                        auth,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(DeleteChartSlice.PTRN_DEL_CHART),
                        MethodRule.DELETE
                    ),
                    new BasicAuthzSlice(
                        new DeleteChartSlice(storage, events, name),
                        auth,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.DELETE)
                        )
                    )
                ),
                new RtRulePath(
                    RtRule.FALLBACK,
                    new SliceSimple(ResponseBuilder.methodNotAllowed().build())
                )
            )
        );
    }
}

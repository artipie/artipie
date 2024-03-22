/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.pypi.http;

import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.BaseResponse;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceDownload;
import com.artipie.http.slice.SliceSimple;
import com.artipie.http.slice.SliceWithHeaders;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.policy.Policy;

import java.util.Optional;
import java.util.Queue;
import java.util.regex.Pattern;

/**
 * PyPi HTTP entry point.
 */
public final class PySlice extends Slice.Wrap {

    /**
     * Primary ctor.
     * @param storage The storage.
     * @param policy Access policy.
     * @param auth Concrete identities.
     * @param name Repository name
     * @param queue Events queue
     */
    public PySlice(
        final Storage storage,
        final Policy<?> policy,
        final Authentication auth,
        final String name,
        final Optional<Queue<ArtifactEvent>> queue
    ) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.GET),
                        new RtRule.ByPath(".*\\.(whl|tar\\.gz|zip|tar\\.bz2|tar\\.Z|tar|egg)")
                    ),
                    new BasicAuthzSlice(
                        new SliceWithHeaders(
                            new SliceDownload(storage),
                            Headers.from(ContentType.mime("application/octet-stream"))
                        ),
                        auth,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.POST),
                        new RtRule.ByHeader(
                            "content-type", Pattern.compile("multipart.*", Pattern.CASE_INSENSITIVE)
                        )
                    ),
                    new BasicAuthzSlice(
                        new WheelSlice(storage, queue, name),
                        auth,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.POST),
                        new RtRule.ByHeader(
                            "content-type", Pattern.compile("text.*", Pattern.CASE_INSENSITIVE)
                        )
                    ),
                    new BasicAuthzSlice(
                        new SearchSlice(storage),
                        auth,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.GET),
                        new RtRule.ByPath("(^\\/)|(.*(\\/[a-z0-9\\-]+?\\/?$))")
                    ),
                    new BasicAuthzSlice(
                        new SliceIndex(storage),
                        auth,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.GET)
                    ),
                    new BasicAuthzSlice(
                        new RedirectSlice(),
                        auth,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    RtRule.FALLBACK,
                    new SliceSimple(BaseResponse.notFound())
                )
            )
        );
    }
}

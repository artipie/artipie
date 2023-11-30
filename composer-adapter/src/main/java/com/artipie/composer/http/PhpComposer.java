/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http;

import com.artipie.composer.Repository;
import com.artipie.http.Slice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.policy.Policy;
import java.util.Optional;
import java.util.Queue;
import java.util.regex.Pattern;

/**
 * PHP Composer repository HTTP front end.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class PhpComposer extends Slice.Wrap {
    /**
     * Ctor.
     * @param repository Repository
     * @param policy Access permissions
     * @param auth Authentication
     * @param name Repository name
     * @param events Artifact repository events
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public PhpComposer(
        final Repository repository,
        final Policy<?> policy,
        final Authentication auth,
        final String name,
        final Optional<Queue<ArtifactEvent>> events
    ) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.Any(
                            new RtRule.ByPath(PackageMetadataSlice.PACKAGE),
                            new RtRule.ByPath(PackageMetadataSlice.ALL_PACKAGES)
                        ),
                        ByMethodsRule.Standard.GET
                    ),
                    new BasicAuthzSlice(
                        new PackageMetadataSlice(repository),
                        auth,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(Pattern.compile("^/?artifacts/.*\\.zip$")),
                        ByMethodsRule.Standard.GET
                    ),
                    new BasicAuthzSlice(
                        new DownloadArchiveSlice(repository),
                        auth,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(AddSlice.PATH_PATTERN),
                        ByMethodsRule.Standard.PUT
                    ),
                    new BasicAuthzSlice(
                        new AddSlice(repository),
                        auth,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(AddArchiveSlice.PATH),
                        ByMethodsRule.Standard.PUT
                    ),
                    new BasicAuthzSlice(
                        new AddArchiveSlice(repository, events, name),
                        auth,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
                        )
                    )
                )
            )
        );
    }
}

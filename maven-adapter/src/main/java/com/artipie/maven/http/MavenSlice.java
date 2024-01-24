/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

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
import com.artipie.maven.asto.AstoMaven;
import com.artipie.maven.asto.AstoValidUpload;
import com.artipie.maven.metadata.ArtifactEventInfo;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.policy.Policy;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.regex.Pattern;

/**
 * Maven API entry point.
 * @since 0.1
 */
public final class MavenSlice extends Slice.Wrap {

    /**
     * Instance of {@link ArtifactEventInfo}.
     */
    public static final ArtifactEventInfo EVENT_INFO = new ArtifactEventInfo();

    /**
     * Supported artifacts extensions. According to
     * <a href="https://maven.apache.org/ref/3.6.3/maven-core/artifact-handlers.html">Artifact
     * handlers</a> by maven-core and <a href="https://maven.apache.org/pom.html">Maven docs</a>.
     */
    public static final List<String> EXT =
        List.of("jar", "war", "maven-plugin", "ejb", "ear", "rar", "zip", "aar", "pom");

    /**
     * Pattern to obtain artifact name and version from key. The regex DOES NOT match
     * checksum files, xmls, javadoc and sources archives. Uses list of supported extensions
     * from above.
     */
    public static final Pattern ARTIFACT = Pattern.compile(
        String.format(
            "^(?<pkg>.+)/.+(?<!sources|javadoc)\\.(?<ext>%s)$", String.join("|", MavenSlice.EXT)
        )
    );

    /**
     * Ctor.
     * @param storage The storage and default parameters for free access.
     */
    public MavenSlice(final Storage storage) {
        this(storage, Policy.FREE, Authentication.ANONYMOUS, "*", Optional.empty());
    }

    /**
     * Private ctor since Artipie doesn't know about `Identities` implementation.
     * @param storage The storage.
     * @param policy Access policy.
     * @param users Concrete identities.
     * @param name Repository name
     * @param events Artifact events
     */
    public MavenSlice(final Storage storage, final Policy<?> policy, final Authentication users,
        final String name, final Optional<Queue<ArtifactEvent>> events) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new RtRule.Any(
                        new ByMethodsRule(RqMethod.GET),
                        new ByMethodsRule(RqMethod.HEAD)
                    ),
                    new BasicAuthzSlice(
                        new LocalMavenSlice(storage),
                        users,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.PUT),
                        new RtRule.ByPath(".*SNAPSHOT.*")
                    ),
                    new BasicAuthzSlice(
                        new UploadSlice(storage),
                        users,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.PUT),
                        new RtRule.ByPath(PutMetadataSlice.PTN_META)
                    ),
                    new BasicAuthzSlice(
                        new PutMetadataSlice(storage),
                        users,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
                        )
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.PUT),
                        new RtRule.ByPath(PutMetadataChecksumSlice.PTN)
                    ),
                    new BasicAuthzSlice(
                        new PutMetadataChecksumSlice(
                            storage, new AstoValidUpload(storage),
                            new AstoMaven(storage), name, events
                        ),
                        users,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
                        )
                    )
                ),
                new RtRulePath(
                    new ByMethodsRule(RqMethod.PUT),
                    new BasicAuthzSlice(
                        new UploadSlice(storage),
                        users,
                        new OperationControl(
                            policy, new AdapterBasicPermission(name, Action.Standard.WRITE)
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

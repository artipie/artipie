/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.Docker;
import com.artipie.docker.http.blobs.GetBlobsSlice;
import com.artipie.docker.http.blobs.HeadBlobsSlice;
import com.artipie.docker.http.manifest.GetManifestSlice;
import com.artipie.docker.http.manifest.HeadManifestSlice;
import com.artipie.docker.http.manifest.PushManifestSlice;
import com.artipie.docker.http.upload.DeleteUploadSlice;
import com.artipie.docker.http.upload.GetUploadSlice;
import com.artipie.docker.http.upload.PatchUploadSlice;
import com.artipie.docker.http.upload.PostUploadSlice;
import com.artipie.docker.http.upload.PutUploadSlice;
import com.artipie.http.Slice;
import com.artipie.http.auth.AuthScheme;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.policy.Policy;

import java.util.Optional;
import java.util.Queue;

/**
 * Slice implementing Docker Registry HTTP API.
 * See <a href="https://docs.docker.com/registry/spec/api/">Docker Registry HTTP API V2</a>.
 */
public final class DockerSlice extends Slice.Wrap {

    /**
     * @param docker Docker repository.
     */
    public DockerSlice(final Docker docker) {
        this(docker, Policy.FREE, AuthScheme.NONE, Optional.empty());
    }

    /**
     * @param docker Docker repository.
     * @param events Artifact events
     */
    public DockerSlice(final Docker docker, final Queue<ArtifactEvent> events) {
        this(docker, Policy.FREE, AuthScheme.NONE, Optional.of(events));
    }

    /**
     * @param docker Docker repository.
     * @param perms Access permissions.
     * @param auth Authentication mechanism used in BasicAuthScheme.
     * @deprecated Use constructor accepting {@link AuthScheme}.
     */
    @Deprecated
    public DockerSlice(final Docker docker, final Policy<?> perms, final Authentication auth) {
        this(docker, perms, new BasicAuthScheme(auth), Optional.empty());
    }

    /**
     * @param docker Docker repository.
     * @param policy Access policy.
     * @param auth Authentication scheme.
     * @param events Artifact events queue.
     */
    public DockerSlice(
        Docker docker, Policy<?> policy, AuthScheme auth,
        Optional<Queue<ArtifactEvent>> events
    ) {
        super(
            new ErrorHandlingSlice(
                new SliceRoute(
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(PathPatterns.BASE),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new BaseSlice(docker), policy, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(PathPatterns.MANIFESTS),
                            new ByMethodsRule(RqMethod.HEAD)
                        ),
                        auth(new HeadManifestSlice(docker), policy, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(PathPatterns.MANIFESTS),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new GetManifestSlice(docker), policy, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(PathPatterns.MANIFESTS),
                            ByMethodsRule.Standard.PUT
                        ),
                        auth(new PushManifestSlice(docker, events.orElse(null)),
                            policy, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(PathPatterns.TAGS),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new TagsSlice(docker), policy, auth)
                    ),

                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(PathPatterns.BLOBS),
                            new ByMethodsRule(RqMethod.HEAD)
                        ),
                        auth(new HeadBlobsSlice(docker), policy, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(PathPatterns.BLOBS),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new GetBlobsSlice(docker), policy, auth)
                    ),

                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(PathPatterns.UPLOADS),
                            ByMethodsRule.Standard.POST
                        ),
                        auth(new PostUploadSlice(docker), policy, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(PathPatterns.UPLOADS),
                            new ByMethodsRule(RqMethod.PATCH)
                        ),
                        auth(new PatchUploadSlice(docker), policy, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(PathPatterns.UPLOADS),
                            ByMethodsRule.Standard.PUT
                        ),
                        auth(new PutUploadSlice(docker), policy, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(PathPatterns.UPLOADS),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new GetUploadSlice(docker), policy, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(PathPatterns.UPLOADS),
                            ByMethodsRule.Standard.DELETE
                        ),
                        auth(new DeleteUploadSlice(docker), policy, auth)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(PathPatterns.CATALOG),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new CatalogSlice(docker), policy, auth)
                    )
                )
            )
        );
    }

    /**
     * Requires authentication and authorization for slice.
     *
     * @param origin Origin slice.
     * @param policy Access permissions.
     * @param auth Authentication scheme.
     * @return Authorized slice.
     */
    private static Slice auth(DockerActionSlice origin, Policy<?> policy, AuthScheme auth) {
        return new DockerAuthSlice(new AuthScopeSlice(origin, auth, policy));
    }
}

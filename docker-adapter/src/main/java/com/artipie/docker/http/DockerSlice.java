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
import com.artipie.http.rt.MethodRule;
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
                    RtRulePath.route(MethodRule.GET, PathPatterns.BASE,
                        auth(new BaseSlice(docker), policy, auth)
                    ),
                    RtRulePath.route(MethodRule.HEAD, PathPatterns.MANIFESTS,
                        auth(new HeadManifestSlice(docker), policy, auth)
                    ),
                    RtRulePath.route(MethodRule.GET, PathPatterns.MANIFESTS,
                        auth(new GetManifestSlice(docker), policy, auth)
                    ),
                    RtRulePath.route(MethodRule.PUT, PathPatterns.MANIFESTS,
                        auth(new PushManifestSlice(docker, events.orElse(null)),
                            policy, auth)
                    ),
                    RtRulePath.route(MethodRule.GET, PathPatterns.TAGS,
                        auth(new TagsSlice(docker), policy, auth)
                    ),
                    RtRulePath.route(MethodRule.HEAD, PathPatterns.BLOBS,
                        auth(new HeadBlobsSlice(docker), policy, auth)
                    ),
                    RtRulePath.route(MethodRule.GET, PathPatterns.BLOBS,
                        auth(new GetBlobsSlice(docker), policy, auth)
                    ),
                    RtRulePath.route(MethodRule.POST, PathPatterns.UPLOADS,
                        auth(new PostUploadSlice(docker), policy, auth)
                    ),
                    RtRulePath.route(MethodRule.PATCH, PathPatterns.UPLOADS,
                        auth(new PatchUploadSlice(docker), policy, auth)
                    ),
                    RtRulePath.route(MethodRule.PUT, PathPatterns.UPLOADS,
                        auth(new PutUploadSlice(docker), policy, auth)
                    ),
                    RtRulePath.route(MethodRule.GET, PathPatterns.UPLOADS,
                        auth(new GetUploadSlice(docker), policy, auth)
                    ),
                    RtRulePath.route(MethodRule.DELETE, PathPatterns.UPLOADS,
                        auth(new DeleteUploadSlice(docker), policy, auth)
                    ),
                    RtRulePath.route(MethodRule.GET, PathPatterns.CATALOG,
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

/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.docker.Docker;
import com.artipie.docker.http.upload.DeleteUploadSlice;
import com.artipie.docker.http.upload.GetUploadSlice;
import com.artipie.docker.http.upload.PatchUploadSlice;
import com.artipie.docker.http.upload.PostUploadSlice;
import com.artipie.docker.http.upload.PutUploadSlice;
import com.artipie.docker.http.upload.UploadSlice;
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
        this(docker, Policy.FREE, AuthScheme.NONE, Optional.empty(), "*");
    }

    /**
     * @param docker Docker repository.
     * @param events Artifact events
     */
    public DockerSlice(final Docker docker, final Queue<ArtifactEvent> events) {
        this(docker, Policy.FREE, AuthScheme.NONE, Optional.of(events), "*");
    }

    /**
     * @param docker Docker repository.
     * @param perms Access permissions.
     * @param auth Authentication mechanism used in BasicAuthScheme.
     * @deprecated Use constructor accepting {@link AuthScheme}.
     */
    @Deprecated
    public DockerSlice(final Docker docker, final Policy<?> perms, final Authentication auth) {
        this(docker, perms, new BasicAuthScheme(auth), Optional.empty(), "*");
    }

    /**
     * @param docker Docker repository.
     * @param policy Access policy.
     * @param auth Authentication scheme.
     * @param events Artifact events queue.
     * @param registryName Docker registry name.
     */
    public DockerSlice(
        Docker docker,
        Policy<?> policy,
        AuthScheme auth,
        Optional<Queue<ArtifactEvent>> events,
        String registryName
    ) {
        super(
            new ErrorHandlingSlice(
                new SliceRoute(
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(BaseEntity.PATH),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new BaseEntity(), policy, auth, registryName)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(ManifestEntity.PATH),
                            new ByMethodsRule(RqMethod.HEAD)
                        ),
                        auth(new ManifestEntity.Head(docker), policy, auth, registryName)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(ManifestEntity.PATH),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new ManifestEntity.Get(docker), policy, auth, registryName)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(ManifestEntity.PATH),
                            ByMethodsRule.Standard.PUT
                        ),
                        new ManifestEntity.PutAuth(
                            docker, new ManifestEntity.Put(docker, events, registryName), auth, policy, registryName
                        )
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(TagsEntity.PATH),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new TagsEntity.Get(docker), policy, auth, registryName)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(BlobEntity.PATH),
                            new ByMethodsRule(RqMethod.HEAD)
                        ),
                        auth(new BlobEntity.Head(docker), policy, auth, registryName)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(BlobEntity.PATH),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new BlobEntity.Get(docker), policy, auth, registryName)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(UploadSlice.PATH),
                            ByMethodsRule.Standard.POST
                        ),
                        auth( new PostUploadSlice(docker), policy, auth, registryName)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(UploadSlice.PATH),
                            new ByMethodsRule(RqMethod.PATCH)
                        ),
                        auth(new PatchUploadSlice(docker), policy, auth, registryName)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(UploadSlice.PATH),
                            ByMethodsRule.Standard.PUT
                        ),
                        auth(new PutUploadSlice(docker), policy, auth, registryName)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(UploadSlice.PATH),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new GetUploadSlice(docker), policy, auth, registryName)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(UploadSlice.PATH),
                            ByMethodsRule.Standard.DELETE
                        ),
                        auth(new DeleteUploadSlice(docker), policy, auth, registryName)
                    ),
                    new RtRulePath(
                        new RtRule.All(
                            new RtRule.ByPath(CatalogEntity.PATH),
                            ByMethodsRule.Standard.GET
                        ),
                        auth(new CatalogEntity.Get(docker), policy, auth, registryName)
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
     * @param registryName Repository name
     * @return Authorized slice.
     */
    private static Slice auth(
        final ScopeSlice origin,
        final Policy<?> policy,
        final AuthScheme auth,
        final String registryName
    ) {
        return new DockerAuthSlice(new AuthScopeSlice(origin, auth, policy, registryName));
    }
}

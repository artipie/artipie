/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.ResponseBuilder;
import com.artipie.nuget.Repository;
import com.artipie.nuget.http.content.PackageContent;
import com.artipie.nuget.http.index.ServiceIndex;
import com.artipie.nuget.http.metadata.PackageMetadata;
import com.artipie.nuget.http.publish.PackagePublish;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.policy.Policy;

import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

/**
 * NuGet repository HTTP front end.
 */
public final class NuGet implements Slice {

    /**
     * Base URL.
     */
    private final URL url;

    /**
     * Repository.
     */
    private final Repository repository;

    /**
     * Access policy.
     */
    private final Policy<?> policy;

    /**
     * User identities.
     */
    private final Authentication users;

    /**
     * Repository name.
     */
    private final String name;

    /**
     * Artifact events.
     */
    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * @param url Base URL.
     * @param repository Storage for packages.
     * @param policy Access policy.
     * @param users User identities.
     * @param name Repository name
     * @param events Events queue
     */
    public NuGet(
        final URL url,
        final Repository repository,
        final Policy<?> policy,
        final Authentication users,
        final String name,
        final Optional<Queue<ArtifactEvent>> events
    ) {
        this.url = url;
        this.repository = repository;
        this.policy = policy;
        this.users = users;
        this.name = name;
        this.events = events;
    }

    @Override
    public CompletableFuture<Response> response(RequestLine line, Headers headers, Content body) {
        final String path = line.uri().getPath();
        final Resource resource = this.resource(path);
        final RqMethod method = line.method();
        if (method.equals(RqMethod.GET)) {
            return resource.get(headers);
        }
        if (method.equals(RqMethod.PUT)) {
            return resource.put(headers, body);
        }
        return ResponseBuilder.methodNotAllowed().completedFuture();
    }

    /**
     * Find resource by relative path.
     *
     * @param path Relative path.
     * @return Resource found by path.
     */
    private Resource resource(final String path) {
        final PackagePublish publish = new PackagePublish(this.repository, this.events, this.name);
        final PackageContent content = new PackageContent(this.url, this.repository);
        final PackageMetadata metadata = new PackageMetadata(this.repository, content);
        return new RoutingResource(
            path,
            new ServiceIndex(
                Arrays.asList(
                    new RouteService(this.url, publish, "PackagePublish/2.0.0"),
                    new RouteService(this.url, metadata, "RegistrationsBaseUrl/Versioned"),
                    new RouteService(this.url, content, "PackageBaseAddress/3.0.0")
                )
            ),
            this.auth(publish, Action.Standard.WRITE),
            this.auth(content, Action.Standard.READ),
            this.auth(metadata, Action.Standard.READ)
        );
    }

    /**
     * Create route supporting basic authentication.
     *
     * @param route Route requiring authentication.
     * @param action Action.
     * @return Authenticated route.
     */
    private Route auth(final Route route, final Action action) {
        return new BasicAuthRoute(
            route,
            new OperationControl(this.policy, new AdapterBasicPermission(this.name, action)),
            this.users
        );
    }
}

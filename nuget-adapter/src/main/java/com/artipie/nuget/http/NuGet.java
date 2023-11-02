/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.nuget.http;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
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
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import org.reactivestreams.Publisher;

/**
 * NuGet repository HTTP front end.
 *
 * @since 0.1
 * @todo #84:30min Refactor NuGet class, reduce number of fields.
 *  There are too many fields and constructor parameters as result in this class.
 *  Probably it is needed to extract some additional abstractions to reduce it,
 *  joint Permissions and Identities might be one of them.
 * @checkstyle ParameterNumberCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (2 lines)
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
     * Ctor.
     *
     * @param url Base URL.
     * @param repository Repository.
     */
    public NuGet(final URL url, final Repository repository) {
        this(url, repository, Policy.FREE, Authentication.ANONYMOUS, "*", Optional.empty());
    }

    /**
     * Ctor.
     *
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
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final Response response;
        final RequestLineFrom request = new RequestLineFrom(line);
        final String path = request.uri().getPath();
        final Resource resource = this.resource(path);
        final RqMethod method = request.method();
        if (method.equals(RqMethod.GET)) {
            response = resource.get(new Headers.From(headers));
        } else if (method.equals(RqMethod.PUT)) {
            response = resource.put(new Headers.From(headers), body);
        } else {
            response = new RsWithStatus(RsStatus.METHOD_NOT_ALLOWED);
        }
        return response;
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

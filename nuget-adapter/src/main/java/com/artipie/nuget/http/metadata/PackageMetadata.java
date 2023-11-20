/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http.metadata;

import com.artipie.nuget.Repository;
import com.artipie.nuget.http.Absent;
import com.artipie.nuget.http.Resource;
import com.artipie.nuget.http.Route;
import com.artipie.nuget.metadata.PackageId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Package metadata route.
 * See <a href="https://docs.microsoft.com/en-us/nuget/api/registration-base-url-resource">Package Metadata</a>
 *
 * @since 0.1
 */
public final class PackageMetadata implements Route {

    /**
     * Base path for the route.
     */
    private static final String BASE = "/registrations";

    /**
     * RegEx pattern for registration path.
     */
    private static final Pattern REGISTRATION = Pattern.compile(
        String.format("%s/(?<id>[^/]+)/index.json$", PackageMetadata.BASE)
    );

    /**
     * Repository to read data from.
     */
    private final Repository repository;

    /**
     * Package content location.
     */
    private final ContentLocation content;

    /**
     * Ctor.
     *
     * @param repository Repository to read data from.
     * @param content Package content storage.
     */
    public PackageMetadata(final Repository repository, final ContentLocation content) {
        this.repository = repository;
        this.content = content;
    }

    @Override
    public String path() {
        return PackageMetadata.BASE;
    }

    @Override
    public Resource resource(final String path) {
        final Matcher matcher = REGISTRATION.matcher(path);
        final Resource resource;
        if (matcher.find()) {
            resource = new Registration(
                this.repository,
                this.content,
                new PackageId(matcher.group("id"))
            );
        } else {
            resource = new Absent();
        }
        return resource;
    }
}

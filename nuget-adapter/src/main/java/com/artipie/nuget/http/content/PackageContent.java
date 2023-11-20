/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http.content;

import com.artipie.asto.Key;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.nuget.PackageIdentity;
import com.artipie.nuget.Repository;
import com.artipie.nuget.http.Resource;
import com.artipie.nuget.http.Route;
import com.artipie.nuget.http.RsWithBodyNoHeaders;
import com.artipie.nuget.http.metadata.ContentLocation;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Optional;
import org.reactivestreams.Publisher;

/**
 * Package content route.
 * See <a href="https://docs.microsoft.com/en-us/nuget/api/package-base-address-resource">Package Content</a>
 *
 * @since 0.1
 */
@SuppressWarnings("deprecation")
public final class PackageContent implements Route, ContentLocation {

    /**
     * Base URL of repository.
     */
    private final URL base;

    /**
     * Repository to read content from.
     */
    private final Repository repository;

    /**
     * Ctor.
     *
     * @param base Base URL of repository.
     * @param repository Repository to read content from.
     */
    public PackageContent(final URL base, final Repository repository) {
        this.base = base;
        this.repository = repository;
    }

    @Override
    public String path() {
        return "/content";
    }

    @Override
    public Resource resource(final String path) {
        return new PackageResource(path, this.repository);
    }

    @Override
    public URL url(final PackageIdentity identity) {
        final String relative = String.format(
            "%s%s/%s",
            this.base.getPath(),
            this.path(),
            identity.nupkgKey().string()
        );
        try {
            return new URL(this.base, relative);
        } catch (final MalformedURLException ex) {
            throw new IllegalStateException(
                String.format("Failed to build URL from base: '%s'", this.base),
                ex
            );
        }
    }

    /**
     * Package content resource.
     *
     * @since 0.1
     */
    private class PackageResource implements Resource {

        /**
         * Resource path.
         */
        private final String path;

        /**
         * Repository to read content from.
         */
        private final Repository repository;

        /**
         * Ctor.
         *
         * @param path Resource path.
         * @param repository Storage to read content from.
         */
        PackageResource(final String path, final Repository repository) {
            this.path = path;
            this.repository = repository;
        }

        @Override
        public Response get(final Headers headers) {
            return this.key().<Response>map(
                key -> new AsyncResponse(
                    this.repository.content(key).thenApply(
                        existing -> existing.<Response>map(
                            data -> new RsWithBodyNoHeaders(new RsWithStatus(RsStatus.OK), data)
                        ).orElse(new RsWithStatus(RsStatus.NOT_FOUND))
                    )
                )
            ).orElse(new RsWithStatus(RsStatus.NOT_FOUND));
        }

        @Override
        public Response put(
            final Headers headers,
            final Publisher<ByteBuffer> body) {
            return new RsWithStatus(RsStatus.METHOD_NOT_ALLOWED);
        }

        /**
         * Tries to build key to storage value from path.
         *
         * @return Key to storage value, if there is one.
         */
        private Optional<Key> key() {
            final String prefix = String.format("%s/", path());
            final Optional<Key> parsed;
            if (this.path.startsWith(prefix)) {
                parsed = Optional.of(new Key.From(this.path.substring(prefix.length())));
            } else {
                parsed = Optional.empty();
            }
            return parsed;
        }
    }
}

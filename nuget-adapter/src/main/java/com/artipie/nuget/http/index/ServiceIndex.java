/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http.index;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.nuget.http.Absent;
import com.artipie.nuget.http.Resource;
import com.artipie.nuget.http.Route;
import com.artipie.nuget.http.RsWithBodyNoHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import org.reactivestreams.Publisher;

/**
 * Service index route.
 * See <a href="https://docs.microsoft.com/en-us/nuget/api/service-index">Service Index</a>
 *
 * @since 0.1
 */
public final class ServiceIndex implements Route {

    /**
     * Services.
     */
    private final Iterable<Service> services;

    /**
     * Ctor.
     *
     * @param services Services.
     */
    public ServiceIndex(final Iterable<Service> services) {
        this.services = services;
    }

    @Override
    public String path() {
        return "/";
    }

    @Override
    public Resource resource(final String path) {
        final Resource resource;
        if (path.equals("/index.json")) {
            resource = new Index();
        } else {
            resource = new Absent();
        }
        return resource;
    }

    /**
     * Services index JSON "/index.json".
     *
     * @since 0.1
     */
    private final class Index implements Resource {

        @Override
        public Response get(final Headers headers) {
            final JsonArrayBuilder resources = Json.createArrayBuilder();
            for (final Service service : ServiceIndex.this.services) {
                resources.add(
                    Json.createObjectBuilder()
                        .add("@id", service.url())
                        .add("@type", service.type())
                );
            }
            final JsonObject json = Json.createObjectBuilder()
                .add("version", "3.0.0")
                .add("resources", resources)
                .build();
            try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                JsonWriter writer = Json.createWriter(out)) {
                writer.writeObject(json);
                out.flush();
                return new RsWithStatus(
                    new RsWithBodyNoHeaders(out.toByteArray()),
                    RsStatus.OK
                );
            } catch (final IOException ex) {
                throw new IllegalStateException("Failed to serialize JSON to bytes", ex);
            }
        }

        @Override
        public Response put(
            final Headers headers,
            final Publisher<ByteBuffer> body) {
            return new RsWithStatus(RsStatus.METHOD_NOT_ALLOWED);
        }
    }
}

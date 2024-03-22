/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.nuget.http.index;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.BaseResponse;
import com.artipie.nuget.http.Absent;
import com.artipie.nuget.http.Resource;
import com.artipie.nuget.http.Route;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Service index route.
 * See <a href="https://docs.microsoft.com/en-us/nuget/api/service-index">Service Index</a>
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
        if ("/index.json".equals(path)) {
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
                return BaseResponse.ok()
                    .body(out.toByteArray());
            } catch (final IOException ex) {
                throw new IllegalStateException("Failed to serialize JSON to bytes", ex);
            }
        }

        @Override
        public Response put(Headers headers, Content body) {
            return BaseResponse.methodNotAllowed();
        }
    }
}

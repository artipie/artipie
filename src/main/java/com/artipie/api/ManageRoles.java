/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.misc.Json2Yaml;
import com.artipie.misc.Yaml2Json;
import com.artipie.settings.users.CrudRoles;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Manage roles from yaml files.
 * @since 0.29
 */
public final class ManageRoles implements CrudRoles {

    /**
     * Storage.
     */
    private final BlockingStorage blsto;

    /**
     * Ctor.
     * @param blsto Blocking storage
     */
    public ManageRoles(final BlockingStorage blsto) {
        this.blsto = blsto;
    }

    @Override
    public JsonArray list() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        for (final Key key : this.blsto.list(new Key.From("roles"))) {
            builder.add(
                Json.createObjectBuilder().add("name",  ManageUsers.nameFromKey(key)).addAll(
                    Json.createObjectBuilder(
                        new Yaml2Json().apply(
                            new String(this.blsto.value(key), StandardCharsets.UTF_8)
                        ).asJsonObject()
                    )
                ).build()
            );
        }
        return builder.build();
    }

    @Override
    public Optional<JsonObject> get(final String rname) {
        return ManageUsers.infoString(keys(rname), this.blsto)
            .map(yaml -> new Yaml2Json().apply(yaml).asJsonObject());
    }

    @Override
    public void addOrUpdate(final JsonObject info, final String rname) {
        this.blsto.save(
            ManageUsers.fileKey(keys(rname), this.blsto),
            new Json2Yaml().apply(info.toString()).toString().getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public void disable(final String rname) {
        ManageUsers.activate(keys(rname), false, this.blsto);
    }

    @Override
    public void enable(final String rname) {
        ManageUsers.activate(keys(rname), true, this.blsto);
    }

    @Override
    public void remove(final String rname) {
        final Pair<Key, Key> keys = keys(rname);
        if (this.blsto.exists(keys.getLeft())) {
            this.blsto.delete(keys.getLeft());
        } else if (this.blsto.exists(keys.getRight())) {
            this.blsto.delete(keys.getRight());
        } else {
            throw new IllegalStateException(String.format("Failed to find role %s", rname));
        }
    }

    /**
     * Possible keys to check role info by.
     * @param rname The role name
     * @return Keys pair with yml and yaml extension
     */
    private static Pair<Key, Key> keys(final String rname) {
        return new ImmutablePair<>(
            new Key.From(String.format("roles/%s.yaml", rname)),
            new Key.From(String.format("roles/%s.yml", rname))
        );
    }
}

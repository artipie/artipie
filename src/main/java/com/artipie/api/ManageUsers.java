/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.asto.misc.UncheckedIOFunc;
import com.artipie.misc.Json2Yaml;
import com.artipie.misc.Yaml2Json;
import com.artipie.settings.users.CrudUsers;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Users from yaml file.
 *
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class ManageUsers implements CrudUsers {

    /**
     * Yaml field name for password.
     */
    private static final String PASS = "pass";

    /**
     * Yaml field name for password type.
     */
    private static final String TYPE = "type";

    /**
     * Storage.
     */
    private final BlockingStorage blsto;

    /**
     * Ctor.
     *
     * @param blsto Storage
     */
    public ManageUsers(final BlockingStorage blsto) {
        this.blsto = blsto;
    }

    @Override
    public JsonArray list() {
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        try {
            for (final Key key : this.blsto.list(new Key.From("users"))) {
                builder.add(
                    jsonFromYaml(
                        new KeyLastPart(key).get().replace(".yaml", "").replace(".yml", ""),
                        Yaml.createYamlInput(
                            new ByteArrayInputStream(this.blsto.value(key))
                        ).readYamlMapping()
                    )
                );
            }
        } catch (final IOException err) {
            throw new UncheckedIOException(err);
        }
        return builder.build();
    }

    @Override
    public Optional<JsonObject> get(final String uname) {
        return this.userInfo(uname).map(yaml -> jsonFromYaml(uname, yaml));
    }

    @Override
    public void addOrUpdate(final JsonObject info, final String uname) {
        this.blsto.save(
            this.userFileKey(uname),
            new Json2Yaml().apply(info.toString()).toString().getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public void disable(final String uname) {
        this.activateUser(uname, false);
    }

    @Override
    public void enable(final String uname) {
        this.activateUser(uname, true);
    }

    @Override
    public void remove(final String uname) {
        this.blsto.delete(this.userFileKeyIfExists(uname));
    }

    @Override
    public void alterPassword(final String uname, final JsonObject info) {
        final Optional<YamlMapping> yaml = this.userInfo(uname);
        if (yaml.isPresent()) {
            YamlMappingBuilder builder = ManageUsers.copyUserInfo(yaml.get());
            builder = builder.add(ManageUsers.PASS, info.getString("new_pass"));
            builder = builder.add(ManageUsers.TYPE, info.getString("new_type"));
            this.blsto.save(
                this.userFileKey(uname),
                builder.build().toString().getBytes(StandardCharsets.UTF_8)
            );
        } else {
            throw new IllegalStateException(String.format("User %s is not found", uname));
        }
    }

    /**
     * Activate user.
     * @param uname User to activate
     * @param enable Enable or disable
     */
    private void activateUser(final String uname, final boolean enable) {
        final Optional<YamlMapping> info = this.userInfo(uname);
        if (info.isPresent()) {
            YamlMappingBuilder builder = ManageUsers.copyUserInfo(info.get());
            builder = builder.add("enabled", String.valueOf(enable));
            this.blsto.save(
                this.userFileKey(uname),
                builder.build().toString().getBytes(StandardCharsets.UTF_8)
            );
        } else {
            throw new IllegalStateException(String.format("User %s does not exists", uname));
        }
    }

    /**
     * Read user info from yaml. Returns empty if user does not exist.
     * @param uname The name of the user
     * @return User info yaml
     */
    private Optional<YamlMapping> userInfo(final String uname) {
        Optional<byte[]> res = Optional.empty();
        final Pair<Key, Key> keys = ManageUsers.keys(uname);
        if (this.blsto.exists(keys.getLeft())) {
            res = Optional.of(this.blsto.value(keys.getLeft()));
        } else if (this.blsto.exists(keys.getRight())) {
            res = Optional.of(this.blsto.value(keys.getRight()));
        }
        return res.map(
            new UncheckedIOFunc<>(
                bytes -> Yaml.createYamlInput(new ByteArrayInputStream(bytes)).readYamlMapping()
            )
        );
    }

    /**
     * Copy user info from yaml.
     * @param yaml Yaml to copy info from
     * @return Builder with existing data
     */
    private static YamlMappingBuilder copyUserInfo(final YamlMapping yaml) {
        YamlMappingBuilder builder = Yaml.createYamlMappingBuilder();
        for (final YamlNode node : yaml.keys()) {
            final String key = node.asScalar().value();
            builder = builder.add(key, yaml.value(key));
        }
        return builder;
    }

    /**
     * Possible keys to check user info by.
     * @param uname The username
     * @return Keys pair with yml and yaml extension
     */
    private static Pair<Key, Key> keys(final String uname) {
        return new ImmutablePair<>(
            new Key.From(String.format("users/%s.yaml", uname)),
            new Key.From(String.format("users/%s.yml", uname))
        );
    }

    /**
     * Key for the user file. No exception thrown, if file does not exist
     * yml extension if returned.
     * @param uname The username
     * @return Key
     */
    private Key userFileKey(final String uname) {
        final Key res;
        final Pair<Key, Key> keys = ManageUsers.keys(uname);
        if (this.blsto.exists(keys.getLeft())) {
            res = keys.getLeft();
        } else {
            res = keys.getRight();
        }
        return res;
    }

    /**
     * Key for the user file. Exception is thrown, if file does not exist.
     * @param uname The username
     * @return Key
     * @throws IllegalStateException If user file not found
     */
    private Key userFileKeyIfExists(final String uname) {
        final Key res;
        final Pair<Key, Key> keys = ManageUsers.keys(uname);
        if (this.blsto.exists(keys.getLeft())) {
            res = keys.getLeft();
        } else if (this.blsto.exists(keys.getRight())) {
            res = keys.getRight();
        } else {
            throw new IllegalStateException(String.format("Failed to find user %s", uname));
        }
        return res;
    }

    /**
     * Transform user info in yaml format to json.
     * @param uname User name
     * @param yaml Yaml info
     * @return User info as json object
     */
    private static JsonObject jsonFromYaml(final String uname, final YamlMapping yaml) {
        final JsonObjectBuilder usr = Json.createObjectBuilder(
            new Yaml2Json().apply(yaml.toString()).asJsonObject()
        );
        usr.remove(ManageUsers.PASS);
        usr.remove(ManageUsers.TYPE);
        return Json.createObjectBuilder().add("name", uname).addAll(usr).build();
    }
}

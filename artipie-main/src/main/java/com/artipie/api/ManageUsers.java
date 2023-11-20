/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
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
 * Users from yaml files.
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
        for (final Key key : this.blsto.list(new Key.From("users"))) {
            builder.add(
                jsonFromYaml(
                    nameFromKey(key),
                    new String(this.blsto.value(key), StandardCharsets.UTF_8)
                )
            );
        }
        return builder.build();
    }

    @Override
    public Optional<JsonObject> get(final String uname) {
        return infoString(ManageUsers.keys(uname), this.blsto)
            .map(yaml -> jsonFromYaml(uname, yaml));
    }

    @Override
    public void addOrUpdate(final JsonObject info, final String uname) {
        this.blsto.save(
            ManageUsers.fileKey(ManageUsers.keys(uname), this.blsto),
            new Json2Yaml().apply(info.toString()).toString().getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public void disable(final String uname) {
        ManageUsers.activate(keys(uname), false, this.blsto);
    }

    @Override
    public void enable(final String uname) {
        ManageUsers.activate(keys(uname), true, this.blsto);
    }

    @Override
    public void remove(final String uname) {
        this.blsto.delete(this.userFileKeyIfExists(uname));
    }

    @Override
    public void alterPassword(final String uname, final JsonObject info) {
        final Pair<Key, Key> keys = ManageUsers.keys(uname);
        final Optional<YamlMapping> yaml = infoYaml(keys, this.blsto);
        if (yaml.isPresent()) {
            YamlMappingBuilder builder = ManageUsers.copyYamlInfo(yaml.get());
            builder = builder.add(ManageUsers.PASS, info.getString("new_pass"));
            builder = builder.add(ManageUsers.TYPE, info.getString("new_type"));
            this.blsto.save(
                fileKey(keys, this.blsto),
                builder.build().toString().getBytes(StandardCharsets.UTF_8)
            );
        } else {
            throw new IllegalStateException(String.format("User %s is not found", uname));
        }
    }

    /**
     * Get name of the file (user or role)from the key.
     * @param key Key to obtain name from
     * @return Name without .yaml or .yml file extension
     */
    static String nameFromKey(final Key key) {
        return new KeyLastPart(key).get().replace(".yaml", "").replace(".yml", "");
    }

    /**
     * Activate use or role. Throws exception if role or user do not exist
     * @param keys Keys to check
     * @param enable Enable or disable
     * @param blsto Storage
     */
    static void activate(final Pair<Key, Key> keys, final boolean enable,
        final BlockingStorage blsto) {
        final Optional<YamlMapping> info = ManageUsers.infoYaml(keys, blsto);
        if (info.isPresent()) {
            YamlMappingBuilder builder = ManageUsers.copyYamlInfo(info.get());
            builder = builder.add("enabled", String.valueOf(enable));
            blsto.save(
                ManageUsers.fileKey(keys, blsto),
                builder.build().toString().getBytes(StandardCharsets.UTF_8)
            );
        } else {
            throw new IllegalStateException(
                String.format("User/role %s does not exists", keys.getLeft())
            );
        }
    }

    /**
     * Read user/role info from yaml. Returns empty if user/role does not exist.
     * @param keys Keys to get yaml info by
     * @param blsto Storage
     * @return User info yaml
     */
    static Optional<YamlMapping> infoYaml(final Pair<Key, Key> keys,
        final BlockingStorage blsto) {
        return ManageUsers.infoString(keys, blsto).map(
            new UncheckedIOFunc<>(
                str -> Yaml.createYamlInput(str).readYamlMapping()
            )
        );
    }

    /**
     * Read user/role info from yaml. Returns empty if user/role does not exist.
     * @param keys Keys to get yaml info by
     * @param blsto Storage
     * @return User info yaml
     */
    static Optional<String> infoString(final Pair<Key, Key> keys,
        final BlockingStorage blsto) {
        Optional<byte[]> res = Optional.empty();
        if (blsto.exists(keys.getLeft())) {
            res = Optional.of(blsto.value(keys.getLeft()));
        } else if (blsto.exists(keys.getRight())) {
            res = Optional.of(blsto.value(keys.getRight()));
        }
        return res.map(bytes -> new String(bytes, StandardCharsets.UTF_8));
    }

    /**
     * Copy user/role info from yaml.
     * @param yaml Yaml to copy info from
     * @return Builder with existing data
     */
    static YamlMappingBuilder copyYamlInfo(final YamlMapping yaml) {
        YamlMappingBuilder builder = Yaml.createYamlMappingBuilder();
        for (final YamlNode node : yaml.keys()) {
            final String key = node.asScalar().value();
            builder = builder.add(key, yaml.value(key));
        }
        return builder;
    }

    /**
     * Key for the user/role file. No exception thrown, if file does not exist.
     * Extension yml is returned as default.
     * @param keys The keys to check
     * @param blsto Storage
     * @return Key
     */
    static Key fileKey(final Pair<Key, Key> keys, final BlockingStorage blsto) {
        final Key res;
        if (blsto.exists(keys.getLeft())) {
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
     * Transform user info in yaml format to json.
     * @param uname User name
     * @param yaml Yaml info
     * @return User info as json object
     */
    private static JsonObject jsonFromYaml(final String uname, final String yaml) {
        final JsonObjectBuilder usr = Json.createObjectBuilder(
            new Yaml2Json().apply(yaml).asJsonObject()
        );
        usr.remove(ManageUsers.PASS);
        usr.remove(ManageUsers.TYPE);
        return Json.createObjectBuilder().add("name", uname).addAll(usr).build();
    }
}

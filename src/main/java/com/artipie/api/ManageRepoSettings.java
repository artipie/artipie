/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.settings.ConfigFile;
import com.artipie.settings.repo.CrudRepoSettings;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Manage repository settings.
 * @since 0.26
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public final class ManageRepoSettings implements CrudRepoSettings {
    /**
     * Not implemented error.
     */
    private static final NotImplementedException NOT_IMPLEMENTED =
        new NotImplementedException("Not implemented yet");

    /**
     * Repository settings storage.
     */
    private final BlockingStorage asto;

    /**
     * Ctor.
     * @param asto Repository settings storage
     */
    public ManageRepoSettings(final BlockingStorage asto) {
        this.asto = asto;
    }

    @Override
    public Collection<String> listAll() {
        return this.list(Key.ROOT);
    }

    @Override
    public Collection<String> list(final String uname) {
        return this.list(new Key.From(uname));
    }

    @Override
    public boolean exists(final String rname) {
        return this.exists(rname, ext -> ext.key(rname));
    }

    @Override
    public boolean exists(final String uname, final String rname) {
        return this.exists(rname, ext -> ext.key(String.format("%s/%s", uname, rname)));
    }

    @Override
    public JsonObject value(final String rname) {
        final byte[] value = this.asto.value(ManageRepoSettings.key(rname));
        return new JsonObject(new String(value));
    }

    @Override
    public JsonObject value(final String uname, final String rname) {
        final byte[] value = this.asto.value(ManageRepoSettings.key(uname, rname));
        return new JsonObject(new String(value));
    }

    @Override
    public void save(final String rname, final JsonObject value) {
        this.asto.save(
            key(rname),
            value.toBuffer().getBytes()
        );
    }

    @Override
    public void save(final String uname, final String rname, final JsonObject value) {
        this.asto.save(
            ManageRepoSettings.key(uname, rname),
            value.toBuffer().getBytes()
        );
    }

    @Override
    public void delete(final String name) {
        throw ManageRepoSettings.NOT_IMPLEMENTED;
    }

    @Override
    public void move(final String name, final String nname) {
        throw ManageRepoSettings.NOT_IMPLEMENTED;
    }

    /**
     * Check if the repository exists.
     * @param rname Repository name.
     * @param key Key producer function.
     * @return True if repository exists
     */
    private boolean exists(final String rname, final Function<ConfigFile.Extension, Key> key) {
        boolean exists = false;
        if (allowedReponame(rname)) {
            for (final ConfigFile.Extension ext : ConfigFile.Extension.values()) {
                if (this.asto.exists(key.apply(ext))) {
                    exists = true;
                }
            }
        }
        return exists;
    }

    /**
     * List existing repositories.
     * @param key Key (ROOT or user name)
     * @return List of the repositories
     */
    private Collection<String> list(final Key key) {
        final Collection<String> res = new ArrayList<>(5);
        for (final Key item : this.asto.list(key)) {
            final String name = item.string();
            // @checkstyle BooleanExpressionComplexityCheck (5 lines)
            if ((name.endsWith(".yaml") || name.endsWith(".yml")) && allowedReponame(name)) {
                res.add(name.replaceAll("\\.yaml|\\.yml", ""));
            }
        }
        return res;
    }

    /**
     * Check if the repository name is allowed.
     * @param rname Repository name.
     * @return True if repository name is allowed
     */
    private static boolean allowedReponame(final String rname) {
        return !rname.contains("_storages")
            && !rname.contains("_permissions")
            && !rname.contains("_credentials");
    }

    /**
     * Key.
     * @param uname User name.
     * @param rname Repository name.
     * @return Key for user' repository
     */
    private static Key key(final String uname, final String rname) {
        return ConfigFile.Extension.YML.key(String.format("%s/%s", uname, rname));
    }

    /**
     * Key.
     * @param rname Repository name.
     * @return Key for repository
     */
    private static Key key(final String rname) {
        return ConfigFile.Extension.YML.key(rname);
    }
}

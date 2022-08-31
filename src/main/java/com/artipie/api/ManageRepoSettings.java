/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.settings.repo.CrudRepoSettings;
import io.vertx.core.json.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Manage repository settings.
 * @since 0.26
 */
@SuppressWarnings("PMD.TooManyMethods")
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
    public boolean exists(final RepositoryName rname) {
        return allowedReponame(rname)
            &&
            (
                this.asto.exists(keys(rname.string()).getLeft())
                    ||
                    this.asto.exists(keys(rname.string()).getRight())
            );
    }

    @Override
    public JsonObject value(final RepositoryName rname) {
        final JsonObject json;
        if (allowedReponame(rname)) {
            final Pair<Key, Key> keys = keys(rname.string());
            if (this.asto.exists(keys.getLeft())) {
                json = new JsonObject(
                    new String(
                        this.asto.value(keys.getLeft()),
                        StandardCharsets.UTF_8
                    )
                );
            } else if (this.asto.exists(keys.getRight())) {
                json = new JsonObject(
                    new String(
                        this.asto.value(keys.getLeft()),
                        StandardCharsets.UTF_8
                    )
                );
            } else {
                json = null;
            }
        } else {
            json = null;
        }
        return json;
    }

    @Override
    public void save(final RepositoryName rname, final JsonObject value) {
        this.asto.save(
            keys(rname.string()).getRight(),
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
    private static boolean allowedReponame(final RepositoryName rname) {
        return allowedReponame(rname.string());
    }

    /**
     * Check if the repository name is allowed.
     * @param name Repository name.
     * @return True if repository name is allowed
     */
    private static boolean allowedReponame(final String name) {
        return !name.contains("_storages")
            && !name.contains("_permissions")
            && !name.contains("_credentials");
    }

    /**
     * Returns a pair of keys, these keys are possible repository settings names.
     * @param name Key name
     * @return Pair of keys
     */
    private static Pair<Key, Key> keys(final String name) {
        return new ImmutablePair<>(
            new Key.From(String.format("%s.yaml", name)),
            new Key.From(String.format("%s.yml", name))
        );
    }
}

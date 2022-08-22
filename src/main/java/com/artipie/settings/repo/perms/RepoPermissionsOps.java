/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo.perms;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonStructure;

/**
 * Operations with repository permissions: read, change, add, delete.
 * @since 0.26
 */
public interface RepoPermissionsOps {

    /**
     * Read permissions of the repository.
     * @param repo Repository name
     * @return Permissions as json object
     */
    JsonObject get(String repo);

    /**
     * Add repository permissions for user.
     * @param repo Repository name
     * @param uid User id (name) to add permissions for
     * @param perms Permissions to add
     */
    void add(String repo, String uid, JsonArray perms);

    /**
     * Removes all the permissions for repository from user. Does nothing if user does not
     * have any permissions in the repository.
     * @param repo Repository name
     * @param uid User id (name) to revoke permission from
     */
    void delete(String repo, String uid);

    /**
     * Patch repository permissions.
     * @param repo Repository name
     * @param perms New permissions
     */
    void patch(String repo, JsonStructure perms);
}

/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo.perms;

import java.util.Collection;

/**
 * Create/Read/Update/Delete operations with repository permissions.
 * @since 0.26
 */
public interface CrudRepoPermissions {

    /**
     * Read permissions of the repository.
     * @param repo Repository name
     * @return Permissions as json object
     */
    Permissions get(String repo);

    /**
     * Add repository permissions for user.
     * @param repo Repository name
     * @param uid User id (name) to add permissions for
     * @param perms Permissions to add
     */
    void add(String repo, String uid, Collection<String> perms);

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
    void patch(String repo, PermissionsPatch perms);

    /**
     * Permissions data transfer object.
     * @since 0.26
     */
    interface Permissions {

        /**
         * Name can be username or group name.
         * @return String name
         */
        String name();

        /**
         * The list of the operations.
         * @return List of ops
         */
        Collection<String> operations();
    }

    /**
     * Permissions patch data transfer object.
     * @since 0.26
     */
    interface PermissionsPatch {

        /**
         * Permissions list to grant.
         * @return Grant permissions list
         */
        Collection<Permissions> grant();

        /**
         * Permissions list to revoke.
         * @return Grant permissions
         */
        Collection<Permissions> revoke();
    }
}

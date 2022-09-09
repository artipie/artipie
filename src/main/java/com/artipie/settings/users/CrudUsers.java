/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.users;

import javax.json.JsonArray;
import javax.json.JsonObject;

/**
 * Create/Read/Update/Delete Artipie users.
 * @since 0.27
 */
public interface CrudUsers {
    /**
     * List existing users.
     * @return Artipie users
     */
    JsonArray list();

    /**
     * Add user.
     * @param info User info (password, email, groups, etc)
     * @param uid User name
     */
    void addOrUpdate(JsonObject info, String uid);

    /**
     * Remove user by name.
     * @param uid User name
     */
    void remove(String uid);

}

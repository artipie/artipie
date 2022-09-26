/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.users;

import java.util.Optional;
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
     * Get user info.
     * @param uname Username
     * @return User info if user is found
     */
    Optional<JsonObject> get(String uname);

    /**
     * Add user.
     * @param info User info (password, email, groups, etc)
     * @param uname User name
     */
    void addOrUpdate(JsonObject info, String uname);

    /**
     * Remove user by name.
     * @param uname User name
     */
    void remove(String uname);

    /**
     * Alter user's password.
     * @param uname Username
     * @param info Json object with new password and type
     */
    void alterPassword(String uname, JsonObject info);

}

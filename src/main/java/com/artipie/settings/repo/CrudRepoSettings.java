/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo;

import io.vertx.core.json.JsonObject;
import java.util.Collection;

/**
 * Create/Read/Update/Delete repository settings.
 * @since 0.26
 */
public interface CrudRepoSettings {

    /**
     * List all existing repositories.
     * @return List of the repositories
     */
    Collection<String> listAll();

    /**
     * List user's repositories.
     * @param uname User id (name)
     * @return List of the repositories
     */
    Collection<String> list(String uname);

    /**
     * Check if the repository exists.
     * @param rname Repository name, for example maven-s3.
     * @return True if repository exists
     */
    boolean exists(String rname);

    /**
     * Check if the repository exists for org layout.
     * @param uname User name.
     * @param rname Repository name.
     * @return True if repository exists
     */
    boolean exists(String uname, String rname);

    /**
     * Get repository settings as json.
     * @param rname Repository name.
     * @return Json repository settings
     */
    JsonObject value(String rname);

    /**
     * Get repository settings as json for org layout.
     * @param uname User name.
     * @param rname Repository name.
     * @return Json repository settings
     */
    JsonObject value(String uname, String rname);

    /**
     * Add new repository.
     * @param rname Repository name.
     * @param value New repository settings
     */
    void save(String rname, JsonObject value);

    /**
     * Add new user repository for org layout.
     * @param uname User name.
     * @param rname Repository name.
     * @param value New repository settings
     */
    void save(String uname, String rname, JsonObject value);

    /**
     * Remove repository.
     * @param name Repository name. The name can be composite: in the case of org layout it will
     *  consist of two parts - username and repo name, for example john/maven-s3.
     */
    void delete(String name);

    /**
     * Move repository and all data.
     * @param name Old repository name. The name can be composite: in the case of org layout it will
     *  consist of two parts - username and repo name, for example john/maven-s3.
     * @param nname New repository name
     */
    void move(String name, String nname);

}

/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo;

import java.util.Collection;
import javax.json.JsonObject;

/**
 * Repository settings operation: list, delete, save operations.
 * @since 0.26
 */
public interface RepoSettingsOps {

    /**
     * List all existing repositories.
     * @return List of the repositories
     */
    Collection<String> listAll();

    /**
     * List user's repositories.
     * @param uid User id (name)
     * @return List of the repositories
     */
    Collection<String> list(String uid);

    /**
     * Check if the repository exists.
     * @param name Repository name. The name can be composite: in the case of org layout it will
     *  consist of two parts - username and repo name, for example john/maven-s3.
     * @return True if repository exists
     */
    boolean exists(String name);

    /**
     * Get repository settings as json.
     * @param name Repository name. The name can be composite: in the case of org layout it will
     *  consist of two parts - username and repo name, for example john/maven-s3.
     * @return Json repository settings
     */
    JsonObject value(String name);

    /**
     * Add new repository.
     * @param name Repository name. The name can be composite: in the case of org layout it will
     *  consist of two parts - username and repo name, for example john/maven-s3.
     * @param value New repository settings
     */
    void save(String name, byte[] value);

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

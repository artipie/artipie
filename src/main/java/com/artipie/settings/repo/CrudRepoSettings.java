/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo;

import com.artipie.api.RepositoryName;
import java.util.Collection;
import javax.json.JsonStructure;

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
     * Checks if repository settings exists by repository name.
     * @param rname Repository name
     * @return True if found
     */
    boolean exists(RepositoryName rname);

    /**
     * Get repository settings as json.
     * @param name Repository name.
     * @return Json repository settings
     */
    JsonStructure value(RepositoryName name);

    /**
     * Add new repository.
     * @param rname Repository name.
     * @param value New repository settings
     */
    void save(RepositoryName rname, JsonStructure value);

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

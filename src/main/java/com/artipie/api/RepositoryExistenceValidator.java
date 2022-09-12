/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.settings.repo.CrudRepoSettings;

/**
 * Validate repository existence in storage.
 * @since 0.26
 */
public final class RepositoryExistenceValidator implements Validator {
    /**
     * Repository settings create/read/update/delete.
     */
    private final CrudRepoSettings crs;

    /**
     * Repository name.
     */
    private final RepositoryName rname;

    /**
     * Ctor.
     * @param crs Repository settings
     * @param rname Repository name
     */
    public RepositoryExistenceValidator(final CrudRepoSettings crs, final RepositoryName rname) {
        this.crs = crs;
        this.rname = rname;
    }

    @Override
    public boolean isValid() {
        return this.crs.exists(this.rname);
    }

    @Override
    public String errorMessage() {
        return String.format("Repository %s does not exist. ", this.rname);
    }
}

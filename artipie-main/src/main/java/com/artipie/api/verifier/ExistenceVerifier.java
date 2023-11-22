/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.api.verifier;

import com.artipie.api.RepositoryName;
import com.artipie.settings.repo.CrudRepoSettings;

/**
 * Validates that repository name exists in storage.
 * @since 0.26
 */
public final class ExistenceVerifier implements Verifier {
    /**
     * Repository name.
     */
    private final RepositoryName rname;

    /**
     * Repository settings CRUD.
     */
    private final CrudRepoSettings crs;

    /**
     * Ctor.
     * @param rname Repository name
     * @param crs Repository settings CRUD
     */
    public ExistenceVerifier(final RepositoryName rname,
        final CrudRepoSettings crs) {
        this.rname = rname;
        this.crs = crs;
    }

    /**
     * Validate repository name exists.
     * @return True if exists
     */
    public boolean valid() {
        return this.crs.exists(this.rname);
    }

    /**
     * Get error message.
     * @return Error message
     */
    public String message() {
        return String.format("Repository %s does not exist. ", this.rname);
    }
}

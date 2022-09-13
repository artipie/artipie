/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import java.util.Set;

/**
 * Checks if the repository name is valid.
 * The name is considered valid if it does not contain
 * reserved words `_storages, _permissions, _credentials` in it.
 * @since 0.26
 */
public final class RepositoryNameValidator implements Validator {
    /**
     * Words that should not be present inside repository name.
     */
    private final Set<String> reserved;

    /**
     * The name to test.
     */
    private final String name;

    /**
     * Ctor.
     * @param name The name to test
     */
    public RepositoryNameValidator(final String name) {
        this.name = name;
        this.reserved = Set.of("_storages", "_permissions", "_credentials");
    }

    /**
     * Ctor.
     * @param name The name to test
     */
    public RepositoryNameValidator(final RepositoryName name) {
        this(name.toString());
    }

    @Override
    public boolean isValid() {
        return this.reserved.stream().filter(this.name::contains).findAny().isEmpty();
    }

    /**
     * Set of reserved words.
     * @return Reserved words
     */
    public Set<String> reservedWords() {
        return this.reserved;
    }

    @Override
    public String errorMessage() {
        return
            new StringBuilder()
                .append(String.format("Wrong repository name '%s'. ", this.name))
                .append("Repository name should not include following words: ")
                .append(this.reservedWords())
                .toString();
    }
}

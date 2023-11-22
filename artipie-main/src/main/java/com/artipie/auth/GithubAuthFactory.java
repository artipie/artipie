/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.auth;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.http.auth.ArtipieAuthFactory;
import com.artipie.http.auth.AuthFactory;
import com.artipie.http.auth.Authentication;

/**
 * Factory for auth from github.
 * @since 0.30
 */
@ArtipieAuthFactory("github")
public final class GithubAuthFactory implements AuthFactory {

    @Override
    public Authentication getAuthentication(final YamlMapping yaml) {
        return new GithubAuth();
    }
}

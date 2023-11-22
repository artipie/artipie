/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings.users;

/**
 * Password format.
 *
 * @since 0.1
 */
public enum PasswordFormat {

    /**
     * Plain password format.
     */
    PLAIN,

    /**
     * Sha256 password format.
     */
    SHA256
}

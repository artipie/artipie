/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package custom.auth.duplicate;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.http.auth.ArtipieAuthFactory;
import com.artipie.http.auth.AuthFactory;
import com.artipie.http.auth.Authentication;

import java.util.Optional;

/**
 * Test auth.
 */
@ArtipieAuthFactory("first")
public final class DuplicateAuth implements AuthFactory {

    @Override
    public Authentication getAuthentication(final YamlMapping conf) {
        return (username, password) -> Optional.empty();
    }
}

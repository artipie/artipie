/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package custom.auth.first;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.http.auth.ArtipieAuthFactory;
import com.artipie.http.auth.AuthFactory;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.Authentication;

import java.util.Optional;

/**
 * Test auth.
 * @since 1.3
 */
@ArtipieAuthFactory("first")
public final class FirstAuthFactory implements AuthFactory {

    @Override
    public Authentication getAuthentication(final YamlMapping conf) {
        return new FirstAuth();
    }

    public static class FirstAuth implements Authentication {
        @Override
        public Optional<AuthUser> user(String username, String password) {
            return Optional.empty();
        }
    }
}

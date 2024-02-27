/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package custom.auth.second;

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
@ArtipieAuthFactory("second")
public final class SecondAuthFactory implements AuthFactory {

    @Override
    public Authentication getAuthentication(final YamlMapping conf) {
        return new SecondAuth();
    }

    public static class SecondAuth implements Authentication {
        @Override
        public Optional<AuthUser> user(String username, String password) {
            return Optional.empty();
        }
    }
}

/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.auth;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.http.auth.Authentication;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Authentication implementation based on yaml file with credentials.
 * @since 0.3
 * @todo #146 - Consider adding opportunity to configure alternative login, for example:
 *  joe:
 *    login: joe@mail.com
 *    pass: "plain:123"
 *  This configuration would mean that Joe should use his email to login instead of username. Do not
 *  forget to validate credentials, logins should be unique.
 */
public final class AuthFromYaml implements Authentication {

    /**
     * Password format.
     */
    private static final Pattern PSWD_FORMAT = Pattern.compile("(plain:|sha256:)(.+)");

    /**
     * YAML credentials settings.
     */
    private final YamlMapping cred;

    /**
     * Ctor.
     * @param cred Credentials settings
     */
    public AuthFromYaml(final YamlMapping cred) {
        this.cred = cred;
    }

    @Override
    public Optional<String> user(final String user, final String pass) {
        final YamlMapping users = this.cred.yamlMapping("credentials");
        Optional<String> res = Optional.empty();
        if (users != null && users.yamlMapping(user) != null) {
            final String stored = users.yamlMapping(user).string("pass");
            if (stored != null && check(stored, pass)) {
                res = Optional.of(user);
            }
        }
        return res;
    }

    /**
     * Checks stored password against the given one.
     * @param stored Password from settings
     * @param given Password to check
     * @return True if passwords are the same
     */
    private static boolean check(final String stored, final String given) {
        boolean res = false;
        final Matcher matcher = AuthFromYaml.PSWD_FORMAT.matcher(stored);
        if (matcher.matches()) {
            final String pswd = matcher.group(2);
            res = stored.startsWith("sha256") && DigestUtils.sha256Hex(given).equals(pswd)
                || given.equals(pswd);
        }
        return res;
    }

}

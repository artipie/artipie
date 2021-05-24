/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.http.auth.Authentication;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Authentication implementation based on yaml file with credentials.
 * @since 0.3
 * @todo #146:30min Consider adding opportunity to configure alternative login, for example:
 *  | joe:
 *  |   login: joe@mail.com
 *  |   pass: "plain:123"
 *  This configuration would mean that Joe should use his email to login instead of username. Do not
 *  forget to validate credentials, logins should be unique.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
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
    public Optional<Authentication.User> user(final String user, final String pass) {
        final YamlMapping users = this.cred.yamlMapping("credentials");
        Optional<Authentication.User> res = Optional.empty();
        if (users != null && users.yamlMapping(user) != null) {
            final String stored = users.yamlMapping(user).string("pass");
            final Optional<String> type = Optional.ofNullable(
                users.yamlMapping(user).string("type")
            );
            if (stored != null && checkPswdByType(stored, type, pass)) {
                res = Optional.of(
                    new User(user, AuthFromYaml.groups(users.yamlMapping(user)))
                );
            }
        }
        return res;
    }

    @Override
    public String toString() {
        return String.format("%s()", this.getClass().getSimpleName());
    }

    /**
     * Checks stored password against the given one with type.
     * @param stored Password from settings
     * @param type Type of Password from settings
     * @param given Password to check
     * @return True if passwords are the same
     */
    private static boolean checkPswdByType(final String stored, final Optional<String> type,
        final String given) {
        boolean sha = false;
        String checkpswd = String.format("madewrong%s", given);
        if (type.isEmpty()) {
            final Matcher matcher = AuthFromYaml.PSWD_FORMAT.matcher(stored);
            if (matcher.matches()) {
                checkpswd = matcher.group(2);
                sha = stored.startsWith("sha256");
            }
        } else {
            checkpswd = stored;
            sha = type.get().equals("sha256");
        }
        return sha && DigestUtils.sha256Hex(given).equals(checkpswd) || given.equals(checkpswd);
    }

    /**
     * Get groups from yaml.
     * @param users Users yaml
     * @return Groups list
     */
    private static List<String> groups(final YamlMapping users) {
        return Optional.ofNullable(users.yamlSequence("groups")).map(
            yaml -> yaml.values().stream().map(node -> node.asScalar().value())
                .collect(Collectors.toList())
        ).orElse(Collections.emptyList());
    }
}

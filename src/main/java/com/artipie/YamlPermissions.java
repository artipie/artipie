/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.Scalar;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permissions;
import java.util.Collection;

/**
 * Repository permissions: this implementation is based on
 * on repository yaml configuration file.
 * @since 0.2
 */
public final class YamlPermissions implements Permissions {

    /**
     * Asterisk wildcard.
     */
    private static final String WILDCARD = "*";

    /**
     * YAML storage settings.
     */
    private final YamlMapping yaml;

    /**
     * Ctor.
     * @param yaml Configuration yaml
     */
    public YamlPermissions(final YamlMapping yaml) {
        this.yaml = yaml;
    }

    @Override
    public boolean allowed(final Authentication.User user, final String action) {
        return check(this.yaml.yamlSequence(YamlPermissions.quoteAsterisks(user.name())), action)
            || check(
                this.yaml.yamlSequence(YamlPermissions.quoteAsterisks(YamlPermissions.WILDCARD)),
                action
            ) || this.checkGroups(user.groups(), action);
    }

    /**
     * Checks whether action is allowed for any group user has.
     * @param groups User groups
     * @param action Action
     * @return True if action is allowed for group
     */
    private boolean checkGroups(final Collection<String> groups, final String action) {
        return groups.stream()
            .map(group -> this.yaml.yamlSequence(String.format("/%s", group)))
            .anyMatch(seq -> YamlPermissions.check(seq, action));
    }

    /**
     * Checks if permissions sequence has a given action.
     * @param seq Permissions
     * @param action Action
     * @return True if action is allowed
     */
    private static boolean check(final YamlSequence seq, final String action) {
        return seq != null && seq.values().stream().map(node -> Scalar.class.cast(node).value())
            .anyMatch(
                item -> item.equals(action) || item.equals(YamlPermissions.WILDCARD)
                    || item.equals(YamlPermissions.quoteAsterisks(YamlPermissions.WILDCARD))
            );
    }

    /**
     * Quotes asterisks * with double quotes.
     * @param val Value
     * @return Quotes value if it was * sign
     */
    private static String quoteAsterisks(final String val) {
        String res = val;
        if (YamlPermissions.WILDCARD.equals(val)) {
            res = "\"*\"";
        }
        return res;
    }

}

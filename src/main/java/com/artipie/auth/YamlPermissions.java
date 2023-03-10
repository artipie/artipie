/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.amihaiemil.eoyaml.Scalar;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.Permissions;

/**
 * Repository permissions: this implementation is based on
 * on repository yaml configuration file.
 * @since 0.2
 * @checkstyle BooleanExpressionComplexityCheck (500 lines)
 */
public final class YamlPermissions implements Permissions {

    /**
     * Asterisk wildcard.
     */
    private static final String WILDCARD = "*";

    /**
     * Asterisk wildcard with ": it's necessary to escape
     * asterisk to obtain yaml sequence.
     */
    private static final String QUOTED_WILDCARD = "\"*\"";

    /**
     * Asterisk wildcard with ": it's necessary to escape
     * asterisk to obtain yaml sequence.
     */
    private static final String ESCAPED_WILDCARD = "\\*";

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
    public boolean allowed(final AuthUser user, final String action) {
        return check(this.yaml.yamlSequence(YamlPermissions.quoteAsterisk(user.name())), action)
            || check(this.yaml.yamlSequence(YamlPermissions.escapeAsterisk(user.name())), action)
            || check(this.yaml.yamlSequence(YamlPermissions.QUOTED_WILDCARD), action)
            || check(this.yaml.yamlSequence(YamlPermissions.ESCAPED_WILDCARD), action);
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
                    || item.equals(YamlPermissions.ESCAPED_WILDCARD)
            );
    }

    /**
     * Escape asterisk with " to obtain yaml sequence.
     * @param value The value to check
     * @return The value or escaped asterisk
     */
    private static String quoteAsterisk(final String value) {
        String res = value;
        if ("*".equals(value)) {
            res = String.format("\"%s\"", value);
        }
        return res;
    }

    /**
     * Escape asterisk with / to obtain yaml sequence.
     * @param value The value to check
     * @return The value or escaped asterisk
     */
    private static String escapeAsterisk(final String value) {
        String res = value;
        if ("*".equals(value)) {
            res = String.format("\\%s", value);
        }
        return res;
    }

}

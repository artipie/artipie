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
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import com.artipie.http.auth.Permissions;
import java.io.File;
import java.io.IOException;

/**
 * Repository permissions: this implementation is based on
 * on repository yaml configuration file.
 * @since 0.2
 */
public final class RpPermissions implements Permissions {

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
     * @param conf Config file
     */
    public RpPermissions(final File conf) {
        this(readYaml(conf));
    }

    /**
     * Ctor.
     * @param yaml Configuration yaml
     */
    public RpPermissions(final YamlMapping yaml) {
        this.yaml = yaml;
    }

    @Override
    public boolean allowed(final String name, final String action) {
        final YamlMapping all = this.yaml.yamlMapping("permissions");
        return check(all.yamlSequence(name), action)
            || check(all.yamlSequence(RpPermissions.WILDCARD), action);
    }

    /**
     * Read provided file into Yaml object.
     * @param conf File
     * @return Yaml mapping
     */
    private static YamlMapping readYaml(final File conf) {
        try {
            return Yaml.createYamlInput(conf).readYamlMapping().yamlMapping("repo");
        } catch (final IOException ex) {
            throw new IllegalArgumentException("Invalid configuration file", ex);
        }
    }

    /**
     * Checks if permissions sequence has a given action.
     * @param seq Permissions
     * @param action Action
     * @return True if action is allowed
     */
    private static boolean check(final YamlSequence seq, final String action) {
        return seq != null && seq.values().stream().map(Object::toString)
            .anyMatch(item -> item.equals(action) || item.equals(RpPermissions.WILDCARD));
    }

}

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
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.util.List;

/**
 * Credentials config yaml.
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class CredsConfigYaml {

    /**
     * Yaml mapping builder.
     */
    private YamlMappingBuilder builder;

    /**
     * Ctor.
     */
    public CredsConfigYaml() {
        this.builder = Yaml.createYamlMappingBuilder();
    }

    /**
     * Adds users to credentials config.
     * @param usernames Names to add
     * @return Itself
     */
    public CredsConfigYaml withUsers(final String... usernames) {
        for (final String name : usernames) {
            this.builder = this.builder.add(
                name, Yaml.createYamlMappingBuilder().add("pass", "pain:123").build()
            );
        }
        return this;
    }

    /**
     * Adds user with groups to credentials config.
     * @param username Name of the user
     * @param groups Groups list
     * @return Itself
     */
    public CredsConfigYaml withUserAndGroups(final String username, final List<String> groups) {
        YamlMappingBuilder user = Yaml.createYamlMappingBuilder().add("pass", "pain:123");
        if (!groups.isEmpty()) {
            YamlSequenceBuilder seq = Yaml.createYamlSequenceBuilder();
            for (final String group : groups) {
                seq = seq.add(group);
            }
            user = user.add("groups", seq.build());
        }
        this.builder = this.builder.add(username, user.build());
        return this;
    }

    /**
     * Saves credentials config to the provided storage by provided name.
     * @param storage Where to save
     * @param key Storage item name
     */
    public void saveTo(final Storage storage, final Key key) {
        storage.save(key, new Content.From(this.toString().getBytes())).join();
    }

    /**
     * Saves credentials config to the provided storage by `_credentials.yaml` name.
     * @param storage Where to save
     */
    public void saveTo(final Storage storage) {
        this.saveTo(storage, new Key.From("_credentials.yaml"));
    }

    /**
     * Credentials config as yaml mapping.
     * @return Instance of {@link YamlMapping}
     */
    public YamlMapping yaml() {
        return Yaml.createYamlMappingBuilder().add("credentials", this.builder.build()).build();
    }

    @Override
    public String toString() {
        return this.yaml().toString();
    }
}

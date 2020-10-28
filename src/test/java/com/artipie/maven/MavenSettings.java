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
package com.artipie.maven;

import java.util.Collections;
import java.util.List;
import org.cactoos.list.ListOf;

/**
 * Class for storing maven settings xml.
 * @since 0.12
 */
public final class MavenSettings {
    /**
     * List with settings.
     */
    private final List<String> settings;

    /**
     * Ctor.
     * @param port Port for repository url.
     */
    public MavenSettings(final int port) {
        this.settings = Collections.unmodifiableList(
            new ListOf<String>(
                "<settings>",
                "    <profiles>",
                "        <profile>",
                "            <id>artipie</id>",
                "            <repositories>",
                "                <repository>",
                "                    <id>my-maven</id>",
                String.format("<url>http://host.testcontainers.internal:%d/my-maven/</url>", port),
                "                </repository>",
                "            </repositories>",
                "        </profile>",
                "    </profiles>",
                "    <activeProfiles>",
                "        <activeProfile>artipie</activeProfile>",
                "    </activeProfiles>",
                "</settings>"
            )
        );
    }

    /**
     * Returns maven settings.
     * @return Maven settings.
     */
    public List<String> value() {
        return this.settings;
    }
}

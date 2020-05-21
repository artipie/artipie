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
import com.artipie.asto.Storage;
import com.artipie.http.auth.Authentication;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Settings built from YAML.
 *
 * @since 0.1
 */
public final class YamlSettings implements Settings {

    /**
     * YAML file content.
     */
    private final String content;

    /**
     * The Vert.x instance.
     */
    private final Vertx vertx;

    /**
     * Ctor.
     * @param content YAML file content.
     * @param vertx The Vert.x instance.
     */
    public YamlSettings(final String content, final Vertx vertx) {
        this.content = content;
        this.vertx = vertx;
    }

    @Override
    public Storage storage() throws IOException {
        return new YamlStorageSettings(
            Yaml.createYamlInput(this.content)
                .readYamlMapping()
                .yamlMapping("meta")
                .yamlMapping("storage"),
                this.vertx
        ).storage();
    }

    @Override
    public List<Authentication> auth() throws IOException {
        //@checkstyle MethodBodyCommentsCheck (11 lines)
        // @todo #146:30min Implement this method to obtain authentications: for now
        //  we have AuthFromEnv, which is always available and should be active by default, and
        //  AuthFromYaml, which can be configured in main artipie config with `credentials` section:
        //  credentials:
        //    # let's support only `file` type for now
        //    type: file
        //    # file location, storage key, relative to `meta.storage.path`
        //    path: _credentials.yml
        //  For details check comments in #146.
        //  After implementing pass found authentications to slices.
        throw new NotImplementedException("Not yet implemented");
    }
}

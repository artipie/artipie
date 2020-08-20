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

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.api.ContentAs;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorageWrapper;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Artipie credentials.
 * @since 0.9
 */
public interface Credentials {

    /**
     * Artipie users list.
     * @return Yaml as completion action
     */
    CompletionStage<List<String>> users();

    /**
     * Adds user to artipie users.
     * @param username User name
     * @param pswd Password
     * @return Completion add action
     */
    CompletionStage<Void> add(String username, String pswd);

    /**
     * Removes user from artipie users.
     * @param username User to delete
     * @return Completion remove action
     */
    CompletionStage<Void> remove(String username);

    /**
     * Credentials from main artipie config.
     * @since 0.9
     */
    final class FromStorageYaml implements Credentials {

        /**
         * Storage.
         */
        private final Storage storage;

        /**
         * Credentials key.
         */
        private final Key key;

        /**
         * Ctor.
         * @param storage Storage
         * @param key Credentials key
         */
        public FromStorageYaml(final Storage storage, final Key key) {
            this.storage = storage;
            this.key = key;
        }

        @Override
        public CompletionStage<List<String>> users() {
            return this.yaml().thenApply(
                yaml -> yaml.yamlMapping("credentials").keys()
                    .stream().map(node -> node.asScalar().value()).collect(Collectors.toList())
            );
        }

        @Override
        public CompletionStage<Void> add(final String username, final String pswd) {
            throw new IllegalArgumentException("Not implemented yet");
        }

        @Override
        public CompletionStage<Void> remove(final String username) {
            throw new IllegalArgumentException("To be implemented later");
        }

        /**
         * Credentials as yaml.
         * @return Completion action with yaml
         */
        public CompletionStage<YamlMapping> yaml() {
            return new RxStorageWrapper(this.storage)
                .value(this.key)
                .to(ContentAs.YAML)
                .to(SingleInterop.get())
                .thenApply(yaml -> (YamlMapping) yaml);
        }
    }
}

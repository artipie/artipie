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

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RepoPermissions.FromSettings}.
 * @since 0.10
 */
class RepoPermissionsFromSettingsTest {

    @Test
    void returnsRepoList() {
        final Storage storage = new InMemoryStorage();
        storage.save(new Key.From("one.yaml"), new Content.From(new byte[]{})).join();
        storage.save(new Key.From("two.yaml"), new Content.From(new byte[]{})).join();
        storage.save(new Key.From("abc"), new Content.From(new byte[]{})).join();
        storage.save(new Key.From("three.yaml"), new Content.From(new byte[]{})).join();
        MatcherAssert.assertThat(
            new RepoPermissions.FromSettings(new Settings.Fake(storage)).repositories()
                .toCompletableFuture().join(),
            Matchers.containsInAnyOrder("one", "two", "three")
        );
    }

}

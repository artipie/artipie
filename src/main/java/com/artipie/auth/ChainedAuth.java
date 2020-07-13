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

import com.artipie.http.auth.Authentication;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Chained authentication provider, composed by multiple
 * authentication providers invoked by user specified order.
 * @since 0.10
 */
public final class ChainedAuth implements Authentication {

    /**
     * Auth providers list.
     */
    private final List<Authentication> list;

    /**
     * New chain from providers.
     * @param providers Providers
     */
    public ChainedAuth(final Authentication... providers) {
        this(Arrays.asList(providers));
    }

    /**
     * New chain from providers list.
     * @param providers List of providers
     */
    public ChainedAuth(final List<Authentication> providers) {
        this.list = Collections.unmodifiableList(providers);
    }

    @Override
    public Optional<String> user(final String username, final String password) {
        Optional<String> result = Optional.empty();
        for (final Authentication auth : this.list) {
            final Optional<String> attempt = auth.user(username, password);
            if (attempt.isPresent()) {
                result = attempt;
                break;
            }
        }
        return result;
    }
}

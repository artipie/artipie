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
import com.artipie.http.auth.Permissions;
import java.util.Arrays;
import java.util.Collection;

/**
 * Permissions combined from other permissions.
 * Combined permissions allow action if any origin permission allows action.
 *
 * @since 0.11
 */
public final class JoinedPermissions implements Permissions {

    /**
     * Origin permissions.
     */
    private final Collection<Permissions> origins;

    /**
     * Ctor.
     *
     * @param origins Origin permissions.
     */
    public JoinedPermissions(final Permissions... origins) {
        this(Arrays.asList(origins));
    }

    /**
     * Ctor.
     *
     * @param origins Origin permissions.
     */
    public JoinedPermissions(final Collection<Permissions> origins) {
        this.origins = origins;
    }

    @Override
    public boolean allowed(final Authentication.User user, final String action) {
        final boolean result;
        if (this.origins.isEmpty()) {
            result = true;
        } else {
            result = this.origins.stream().anyMatch(origin -> origin.allowed(user, action));
        }
        return result;
    }
}

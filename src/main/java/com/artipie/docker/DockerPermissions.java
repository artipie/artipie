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
package com.artipie.docker;

import com.artipie.docker.http.Scope;
import com.artipie.http.auth.Action;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;

/**
 * Docker permissions adapter that translates Docker scope actions to Artipie actions.
 *
 * @since 0.15
 */
public final class DockerPermissions implements Permissions {

    /**
     * Origin permissions.
     */
    private final Permissions origin;

    /**
     * Ctor.
     *
     * @param origin Origin permissions.
     */
    public DockerPermissions(final Permissions origin) {
        this.origin = origin;
    }

    @Override
    public boolean allowed(final Authentication.User user, final String action) {
        final Action translated;
        switch (new Scope.FromString(action).action()) {
            case "pull":
            case "*":
                translated = Action.Standard.READ;
                break;
            case "push":
                translated = Action.Standard.WRITE;
                break;
            default:
                throw new IllegalArgumentException(String.format("Unexpected action: %s", action));
        }
        return new Permission.ByName(this.origin, translated).allowed(user);
    }
}

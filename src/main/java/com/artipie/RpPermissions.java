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

import com.artipie.http.auth.Permissions;
import java.io.File;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Repository permissions: this implementation is based on
 * on repository yaml configuration file.
 * @since 0.2
 * @todo #69:30min Implement this interface to read and process permissions from
 *  repository yaml configuration file. Test is already implemented, see {@link RpPermissionsTest},
 *  don't forget to enable is when this class is ready. Remove also PMD suppressions please.
 *  For more details check issue #69.
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
public final class RpPermissions implements Permissions {

    /**
     * Repository yaml configuration file.
     */
    private final File conf;

    /**
     * Ctor.
     * @param conf Conf file
     */
    public RpPermissions(final File conf) {
        this.conf = conf;
    }

    @Override
    public boolean allowed(final String name, final String action) {
        throw new NotImplementedException("this method is not yet implemented");
    }

}

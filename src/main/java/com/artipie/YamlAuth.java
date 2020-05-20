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
import com.artipie.http.auth.Authentication;
import java.util.Optional;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Authentication implementation based on yaml file with credentials.
 * @since 0.3
 * @todo #146:30min Implements this class to find user by credentials and enable test. For more
 *  details check #146 and note that password is Base64 encoded.
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
public final class YamlAuth implements Authentication {

    /**
     * YAML credentials settings.
     */
    private final YamlMapping cred;

    /**
     * Ctor.
     * @param cred Credentials settings
     */
    public YamlAuth(final YamlMapping cred) {
        this.cred = cred;
    }

    @Override
    public Optional<String> user(final String user, final String pass) {
        throw new NotImplementedException("Not yet implemented");
    }
}

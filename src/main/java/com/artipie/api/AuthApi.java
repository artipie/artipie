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
package com.artipie.api;

import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicIdentities;
import com.artipie.http.auth.Identities;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqHeaders;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/**
 * API authentication wrapper.
 * @since 0.6
 */
public final class AuthApi implements Identities {

    /**
     * URI path pattern.
     */
    private static final Pattern PTN_PATH =
        Pattern.compile("(?:/api/repos)?/(?<user>[^/.]+)(?:/.*)?");

    /**
     * Origin authentication.
     */
    private final Authentication auth;

    /**
     * Wraps authentication with API restrictions.
     * @param auth Origin
     */
    public AuthApi(final Authentication auth) {
        this.auth = auth;
    }

    @Override
    public Optional<String> user(final String line,
        final Iterable<Map.Entry<String, String>> headers) {
        final Matcher matcher = PTN_PATH.matcher(new RequestLineFrom(line).uri().getPath());
        final Optional<String> res;
        if (matcher.matches()) {
            res = Optional.ofNullable(
                AuthApi.cookies(new RqHeaders(headers, "Cookie")).get("session")
            ).map(AuthApi::session)
                .orElse(new BasicIdentities(this.auth).user(line, headers))
                .filter(user -> user.equals(matcher.group("user")));
        } else {
            res = Optional.empty();
        }
        return res;
    }

    /**
     * Map of cookies.
     * @param raw Raw strings of cookie headers
     * @return Cookies map
     */
    private static Map<String, String> cookies(final Iterable<String> raw) {
        final Map<String, String> map = new HashMap<>(0);
        for (final String value : raw) {
            for (final String pair : value.split(";")) {
                final String[] parts = pair.split("=", 2);
                final String key = parts[0].trim().toLowerCase(Locale.US);
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    map.put(key, parts[1].trim());
                } else {
                    map.remove(key);
                }
            }
        }
        return map;
    }

    /**
     * Decode session id to user name.
     * <p>
     * Encoded session string is hex of user id encrypted with RSA public key.
     * See cipher and key spec format for more details.
     * </p>
     * @param encoded Encoded string
     * @return User id
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    private static Optional<String> session(final String encoded) {
        final String env = System.getenv("ARTIPIE_SESSION_KEY");
        final Optional<String> user;
        if (env == null) {
            user = Optional.empty();
        } else {
            final byte[] key;
            try {
                key = Files.readAllBytes(Paths.get(env));
                final KeySpec spec = new PKCS8EncodedKeySpec(key);
                final Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
                rsa.init(Cipher.DECRYPT_MODE, KeyFactory.getInstance("RSA").generatePrivate(spec));
                user = Optional.of(
                    new String(
                        rsa.doFinal(Hex.decodeHex(encoded.toCharArray())),
                        StandardCharsets.UTF_8
                    )
                );
            } catch (final IOException | DecoderException | GeneralSecurityException err) {
                Logger.error(AuthApi.class, "Failed to read session cookie: %[exception]s");
                throw new IllegalStateException("Failed to read session cookie");
            }
        }
        return user;
    }
}

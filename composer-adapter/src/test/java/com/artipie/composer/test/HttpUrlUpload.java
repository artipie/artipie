/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.test;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import org.apache.commons.codec.binary.Base64;

/**
 * HTTP URL connection for using in tests.
 * @since 0.4
 */
public class HttpUrlUpload {
    /**
     * Url for connection.
     */
    private final String url;

    /**
     * Content that should be uploaded by url.
     */
    private final byte[] content;

    /**
     * Ctor.
     * @param url Url for connection
     * @param content Content that should be uploaded by url
     */
    public HttpUrlUpload(final String url, final byte[] content) {
        this.url = url;
        this.content = Arrays.copyOf(content, content.length);
    }

    /**
     * Upload content to specified url with set permissions for user.
     * @param user User for basic authentication
     * @throws Exception In case of fail to upload
     */
    public void upload(final Optional<TestAuthentication.User> user) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(this.url).toURL().openConnection();
            conn.setRequestMethod("PUT");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            if (user.isPresent()) {
                conn.addRequestProperty(
                    "Authorization",
                    String.format(
                        "Basic %s",
                        new String(
                            Base64.encodeBase64(
                                String.format(
                                    "%s:%s",
                                    user.get().name(),
                                    user.get().password()
                                ).getBytes(StandardCharsets.UTF_8)
                            )
                        )
                    )
                );
            }
            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.write(this.content);
                dos.flush();
            }
            final int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_CREATED) {
                throw new IllegalStateException(
                    String.format("Failed to upload package: %d", status)
                );
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}

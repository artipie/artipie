/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Key;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.http.Headers;
import com.artipie.http.headers.Header;
import com.google.common.base.Strings;

import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Artifact response headers for {@code GET} and {@code HEAD} requests.
 * <p>
 * Maven client supports {@code X-Checksum-*} headers for different hash algorithms,
 * {@code ETag} header for caching, {@code Content-Type} and {@code Content-Disposition}.
 */
@SuppressWarnings({"PMD.UseUtilityClass", "PMD.ProhibitPublicStaticMethods"})
final class ArtifactHeaders {

    /**
     * Headers from artifact key and checksums.
     * @param location Artifact location
     * @param checksums Artifact checksums
     */
    public static Headers from(Key location, Map<String, String> checksums) {
        return new Headers()
            .add(contentDisposition(location))
            .add(contentType(location))
            .addAll(new Headers(checksumsHeader(checksums)));
    }

    /**
     * Content disposition header.
     * @param location Artifact location
     * @return Headers with content disposition
     */
    private static Header contentDisposition(final Key location) {
        return new Header(
            "Content-Disposition",
            String.format("attachment; filename=\"%s\"", new KeyLastPart(location).get())
        );
    }

    /**
     * Checksum headers.
     * @param checksums Artifact checksums
     * @return Checksum header and {@code ETag} header
     */
    private static List<Header> checksumsHeader(final Map<String, String> checksums) {
        List<Header> res = new ArrayList<>(checksums.size() + 1);
        res.addAll(
            checksums.entrySet()
                .stream()
                .map(entry -> new Header("X-Checksum-" + entry.getKey(), entry.getValue()))
                .toList()
        );
        String sha1 = checksums.get("sha1");
        if (!Strings.isNullOrEmpty(sha1)) {
            res.add(new Header("ETag", sha1));
        }
        return res;
    }

    /**
     * Artifact content type header.
     * @param key Artifact key
     * @return Content type header
     */
    private static Header contentType(final Key key) {
        final String type;
        final String src = key.string();
        type = switch (extension(key)) {
            case "jar" -> "application/java-archive";
            case "pom" -> "application/x-maven-pom+xml";
            default -> URLConnection.guessContentTypeFromName(src);
        };
        return new Header("Content-Type", Optional.ofNullable(type).orElse("*"));
    }

    /**
     * Artifact extension.
     * @param key Artifact key
     * @return Lowercased extension without dot char.
     */
    private static String extension(final Key key) {
        final String src = key.string();
        return src.substring(src.lastIndexOf('.') + 1).toLowerCase(Locale.US);
    }
}

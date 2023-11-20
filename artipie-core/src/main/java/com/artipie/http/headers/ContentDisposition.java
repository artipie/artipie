/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.headers;

import com.artipie.http.Headers;
import com.artipie.http.rq.RqHeaders;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Content-Disposition header.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition"></a>
 * @since 0.17.8
 */
public final class ContentDisposition extends Header.Wrap {

    /**
     * Header name.
     */
    public static final String NAME = "Content-Disposition";

    /**
     * Header directives pattern.
     */
    private static final Pattern DIRECTIVES = Pattern.compile(
        "(?<key> \\w+ ) (?:= [\"] (?<value> [^\"]+ ) [\"] )?[;]?",
        Pattern.COMMENTS
    );

    /**
     * Parsed directives.
     */
    private final Map<String, String> directives;

    /**
     * Ctor.
     *
     * @param value Header value.
     */
    public ContentDisposition(final String value) {
        super(new Header(ContentDisposition.NAME, value));
        this.directives = this.parse();
    }

    /**
     * Ctor.
     *
     * @param headers Headers to extract header from.
     */
    public ContentDisposition(final Headers headers) {
        this(new RqHeaders.Single(headers, ContentDisposition.NAME).asString());
    }

    /**
     * The original name of the file transmitted.
     *
     * @return String.
     */
    public String fileName() {
        return this.directives.get("filename");
    }

    /**
     * The name of the HTML field in the form
     * that the content of this subpart refers to.
     *
     * @return String.
     */
    public String fieldName() {
        return this.directives.get("name");
    }

    /**
     * Inline.
     *
     * @return Boolean flag.
     */
    public Boolean isInline() {
        return this.directives.containsKey("inline");
    }

    /**
     * Inline.
     *
     * @return Boolean flag.
     */
    public Boolean isAttachment() {
        return this.directives.containsKey("attachment");
    }

    /**
     * Parse header value to a map.
     *
     * @return Map of keys and values.
     */
    private Map<String, String> parse() {
        final Matcher matcher = ContentDisposition.DIRECTIVES.matcher(this.getValue());
        final Map<String, String> values = new HashMap<>();
        while (matcher.find()) {
            values.put(matcher.group("key"), matcher.group("value"));
        }
        return values;
    }
}

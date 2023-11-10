/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.gem.http;

import com.artipie.gem.GemMeta.MetaInfo;
import com.artipie.gem.JsonMetaFormat;
import com.artipie.gem.YamlMetaFormat;
import com.artipie.http.ArtipieHttpException;
import com.artipie.http.Response;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.rs.common.RsJson;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.function.Function;
import javax.json.Json;
import javax.json.JsonObjectBuilder;

/**
 * Gem meta response format.
 * @since 1.3
 * @checkstyle ClassDataAbstractionCouplingCheck (100 lines)
 */
enum MetaResponseFormat implements Function<MetaInfo, Response> {
    /**
     * JSON response format.
     */
    JSON {
        @Override
        public Response apply(final MetaInfo meta) {
            final JsonObjectBuilder json = Json.createObjectBuilder();
            meta.print(new JsonMetaFormat(json));
            return new RsJson(json.build());
        }
    },

    /**
     * Yaml response format.
     */
    YAML {
        @Override
        public Response apply(final MetaInfo meta) {
            final YamlMetaFormat.Yamler yamler = new YamlMetaFormat.Yamler();
            meta.print(new YamlMetaFormat(yamler));
            final Charset charset = StandardCharsets.UTF_8;
            return new RsWithHeaders(
                new RsWithBody(StandardRs.OK, yamler.build().toString(), charset),
                new ContentType(
                    String.format(
                        "text/x-yaml;charset=%s",
                        charset.displayName().toLowerCase(Locale.US)
                    )
                )
            );
        }
    };

    /**
     * Format by name.
     * @param name Format name
     * @return Response format
     */
    static MetaResponseFormat byName(final String name) {
        final MetaResponseFormat res;
        switch (name) {
            case "json":
                res = MetaResponseFormat.JSON;
                break;
            case "yaml":
                res = MetaResponseFormat.YAML;
                break;
            default:
                throw new ArtipieHttpException(
                    RsStatus.BAD_REQUEST, String.format("unsupported format type `%s`", name)
                );
        }
        return res;
    }
}
